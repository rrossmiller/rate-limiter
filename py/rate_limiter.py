import asyncio
import time
from typing import Callable

# import tiktoken
from tqdm import tqdm


class RateLimiter:
    lock = asyncio.Lock()

    def __init__(self, rpm: int):
        """
        Set the TPM and the RPM given 6 RPM per 1000 TPM and return the rate limiter instance.

        Args:
            tpm (int): tokens per minute
        """
        # self.tpm = tpm
        # self.rpm = int(6 * self.tpm / 1000)
        self.rpm = rpm
        # logging.debug(f"RateLimiter TPM: {self.tpm} | RPM: {self.rpm}")
        # print(f"TPM: {self.tpm} | RPM: {self.rpm}")
        print(f"RPM: {self.rpm}")
        self.period = 60  # seconds
        self.times: list[float] = []
        self.tokens: list[int] = []
        self.spacing = self.period / self.rpm  # + 0.01
        # encoding = tiktoken.encoding_for_model("gpt-4")

    async def rate_limit(
        self,
        func: Callable,
        args: tuple,
        worker: str,
        bar: tqdm | None = None,
    ):
        """
        Rate limit the function call to the specified RPM

        Params:
            func (Callable): the function to rate limit
            args (tuple): the arguments to pass to the function
            worker (str): the name of the worker calling the function
        """
        if bar is not None:
            bar.set_description_str(f"{worker} waiting for lock")
        async with self.lock:
            self.times.append(time.monotonic())

            # Space requests s.t. there are 60/rpm seconds in between each request
            # That allows for the max requests per minute (plus some breathing room)

            # Don't wait on the first request
            # Don't wait on requests that are beyond 1 period from the previous request
            #     For example, in an api, if the last request was yesterday at 5PM and there was no activiy over night.
            if len(self.times) > 1 and (self.times[-1] - self.times[-2]) <= self.period:
                if bar is not None:
                    bar.set_description_str(
                        f"******** {worker} sleeping for {self.spacing:.3f} *******"
                    )
                await asyncio.sleep(self.spacing)
                self.times[-1] = time.monotonic()  # set new time, since it had to wait

            # periodically clean out self.times
            elif len(self.times) > 100:
                for i, t in enumerate(self.times[::-1]):
                    if (self.times[-1] - t) >= self.period:
                        self.times = self.times[-i + 1 :]
                        break

        return await func(*args, bar=bar, worker=worker)


async def worker(
    n: int,
    rl: RateLimiter,
    task_queue: asyncio.Queue,
    ttl_tracker: tqdm | None = None,
    worker_bars=False,
):
    worker_name = f"worker-{n+1}"
    worker_name += " " if n + 1 < 10 else ""
    responses = []
    i = 0

    bar = None
    if worker_bars:
        offset = 0
        if ttl_tracker is not None:
            offset = 1
        bar = tqdm(position=n + offset, desc=worker_name, unit=" tasks")

    while not task_queue.empty():
        # get the next task
        func, args = await task_queue.get()
        response = await rl.rate_limit(func, args, worker_name, bar)
        responses.append(response)
        i += 1
        if bar is not None:
            bar.update()
        if ttl_tracker is not None:
            ttl_tracker.update()
        task_queue.task_done()
    if bar is not None:
        bar.set_description_str(f"{worker_name}: completed")
    # collate results
    results = []
    for r in responses:
        results.append(r)

    return bar, results
