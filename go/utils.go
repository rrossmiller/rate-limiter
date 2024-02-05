package main

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
)

func results(client *http.Client) {
	resp, err := client.Get(fmt.Sprintf("http://localhost:3000/results"))
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		panic(err)
	}
	var res Results
	err = json.Unmarshal(body, &res)
	if err != nil {
		panic(err)
	}
	parsed, _ := json.MarshalIndent(res, "", "  ")
	fmt.Println()
	fmt.Println(string(parsed))

}

func clear(client *http.Client) error {
	req, err := http.NewRequest("DELETE", "http://localhost:3000", nil)
	if err != nil {
		panic(err)
	}
	resp, err := client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return err
	}
	fmt.Println("cleared:", string(body))
	return nil
}
