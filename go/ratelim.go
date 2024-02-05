package main

import (
	"fmt"
	"sync"
	"time"
)

type RateLimiter struct {
	mu       sync.Mutex
	rpm      int
	period   time.Duration
	lastTime *time.Time
	spacing  time.Duration // minimum time between requests
}

func (r *RateLimiter) Default(rpm int) {
	r.rpm = rpm
	r.period = 1 * time.Minute
	x := float64(60) / float64(rpm)
	d, err := time.ParseDuration(fmt.Sprintf("%vs", x))
	if err != nil {
		panic(err)
	}

	r.spacing = d
	fmt.Println("RPM:", r.rpm)
	fmt.Println("Spacing:", r.spacing)
}

func (r *RateLimiter) RateLimit() {

	//lock
	r.mu.Lock()
	defer r.mu.Unlock()
	now := time.Now()

	// if the last time was less than the spacing a wait must happen
	if r.lastTime != nil && now.Sub(*r.lastTime) <= r.spacing {
		w := r.spacing - now.Sub(*r.lastTime)
		// fmt.Println("wait for", w)
		time.Sleep(w)
	}

	t := time.Now()
	r.lastTime = &t
}
