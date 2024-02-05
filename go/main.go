package main

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"sync"

	"github.com/schollz/progressbar/v3"
)

func main() {
	client := &http.Client{}
	err := clear(client)
	if err != nil {
		panic(err)
	}

	rl := RateLimiter{}
	rl.Default(600)
	nTasks := 600

	run(nTasks, client, &rl)
	results(client)

	fmt.Println()
	err = clear(client)
	if err != nil {
		panic(err)
	}
	runPool(nTasks, 12, client, &rl)
	results(client)
}

// One worker per request
func run(nTasks int, client *http.Client, rl *RateLimiter) {
	results := make(chan map[string]int, nTasks)
	bar := progressbar.Default(int64(nTasks))

	var wg sync.WaitGroup
	for i := 0; i < nTasks; i++ {
		wg.Add(1)
		go func(x int) {
			defer wg.Done()

			// limit
			rl.RateLimit()
			resp, err := client.Get(fmt.Sprintf("http://localhost:3000?i=%d", x))
			if err != nil {
				panic(err)
			}
			defer resp.Body.Close()
			body, err := io.ReadAll(resp.Body)
			if err != nil {
				panic(err)
			}
			var respMap map[string]int
			if err := json.Unmarshal(body, &respMap); err == nil {
				results <- respMap
			}
			bar.Add(1)
		}(i)
	}
	wg.Wait()
	close(results)
}

// worker pool
func runPool(nTasks, nWorkers int, client *http.Client, rl *RateLimiter) {
	jobs := make(chan int, nTasks)
	results := make(chan map[string]int, nTasks)
	bar := progressbar.Default(int64(nTasks))

	var wg sync.WaitGroup

	for i := 0; i < nTasks; i++ {
		jobs <- i
	}
	close(jobs)

	for i := 0; i < min(nWorkers, nTasks); i++ {
		wg.Add(1)
		go worker(rl, client, jobs, results, &wg, bar)
	}
	wg.Wait()
	close(results)
}

func worker(rl *RateLimiter, client *http.Client, jobs chan int, results chan map[string]int, wg *sync.WaitGroup, bar *progressbar.ProgressBar) {
	defer wg.Done()

	for x := range jobs {
		// limit
		rl.RateLimit()
		resp, err := client.Get(fmt.Sprintf("http://localhost:3000?i=%d", x))
		if err != nil {
			panic(err)
		}
		defer resp.Body.Close()
		body, err := io.ReadAll(resp.Body)
		if err != nil {
			panic(err)
		}
		var respMap map[string]int
		if err := json.Unmarshal(body, &respMap); err == nil {
			results <- respMap
		}
		bar.Add(1)
	}
}
