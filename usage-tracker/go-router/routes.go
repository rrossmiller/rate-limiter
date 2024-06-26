package main

import (
	"encoding/json"
	"log"
	"math/rand/v2"
	"net/http"

	"fmt"
	"time"
)

func results(w http.ResponseWriter, r *http.Request) {
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
}
func del(w http.ResponseWriter, r *http.Request) {
	fmt.Println("clearing")
	l.mu.Lock()
	defer l.mu.Unlock()
	l.reqs = []time.Time{}
	mt, _ := json.Marshal(l.reqs)
	w.Write([]byte(mt))
}
func root(w http.ResponseWriter, r *http.Request) {
	//TODO: get i from params to keep track of the order of requests
	i := r.URL.Query().Get("i")
	l.mu.Lock()
	l.reqs = append(l.reqs, time.Now())
	l.mu.Unlock()
	sleep := time.Duration(rand.Float64()*5000) * time.Millisecond
	log.Printf("Sleeping for %v, %q", sleep, i)
	time.Sleep(sleep)
	// w.Write([]byte(fmt.Sprintf(`{"Requests": "%v"}`, l.reqs[len(l.reqs)-1])))
	w.Write([]byte(fmt.Sprintf(`{"id": %v}`, i)))

}
