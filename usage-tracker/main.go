package main

import (
	"encoding/json"
	"net/http"

	"fmt"
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"sync"
	"time"
)

type LimitTracker struct {
	mu   sync.Mutex
	reqs []time.Time
}
type Results struct {
	Start         time.Time `json:"start"`
	End           time.Time `json:"end"`
	TotalSeconds  float64   `json:"totalSeconds"`
	RpmPerTenSec  []float64 `json:"rpmPerTenSec"`
	Rpm           []float64 `json:"rpm"`
	TotalRequests int       `json:"totalRequests"`
	RpmPerSec     []float64 `json:"rpmPerSec"`
}

func main() {
	l := LimitTracker{}
	r := chi.NewRouter()
	r.Use(middleware.Logger)

	// record the request
	r.Get("/", func(w http.ResponseWriter, r *http.Request) {
		//TODO: get i from params to keep track of the order of requests
		i := r.URL.Query().Get("i")

		l.mu.Lock()
		l.reqs = append(l.reqs, time.Now())
		l.mu.Unlock()
		// w.Write([]byte(fmt.Sprintf(`{"Requests": "%v"}`, l.reqs[len(l.reqs)-1])))
		w.Write([]byte(fmt.Sprintf(`{"id": %v}`, i)))

	})

	r.Delete("/", func(w http.ResponseWriter, r *http.Request) {
		l.mu.Lock()
		defer l.mu.Unlock()
		l.reqs = []time.Time{}
		mt, _ := json.Marshal(l.reqs)
		w.Write([]byte(mt))
	})

	// collate the results
	r.Get("/results", func(w http.ResponseWriter, r *http.Request) {
		l.mu.Lock()
		defer l.mu.Unlock()
		if len(l.reqs) == 0 {
			w.Write([]byte("No requests yet"))
			return
		}
		// create the results struct
		ttl := float64(l.reqs[len(l.reqs)-1].Sub(l.reqs[0]).Milliseconds())
		results := Results{Start: l.reqs[0], End: l.reqs[len(l.reqs)-1], TotalRequests: len(l.reqs), TotalSeconds: ttl / 1000}

		// calculate the requests per second for each second
		seconds := map[int]int{}
		seconds[0] = 1
		for i := 1; i < len(l.reqs); i++ {
			// what second is the request in
			idx := int(l.reqs[i].Sub(l.reqs[0]).Seconds())
			seconds[idx] += 1
		}
		// extrapolate the rps to rpm
		for _, v := range seconds {
			results.RpmPerSec = append(results.RpmPerSec, float64(v*60))
		}

		// calculate the rpm for each 10 seconds
		for i := 0; i < len(seconds); i += 10 {
			// # reqs over 10 seconds
			n := 0
			for j := i; j < i+10; j++ {
				n += seconds[j]
			}
			results.RpmPerTenSec = append(results.RpmPerTenSec, float64(n*6))
		}
		// calculate the rpm for each minute
		for i := 0; i < len(seconds); i += 60 {
			// # reqs over 10 seconds
			n := 0
			for j := i; j < i+60 && j < len(seconds); j++ {
				n += seconds[j]
			}
			results.Rpm = append(results.Rpm, float64(n))
		}

		jsonResults, err := json.MarshalIndent(results, "", "  ")
		if err != nil {
			panic(err)
		}
		w.Write(jsonResults)
	})

	fmt.Println("Server running on port 3000")
	http.ListenAndServe(":3000", r)
}
