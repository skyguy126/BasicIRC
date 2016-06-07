package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"strings"
	"sync"
	"time"
	"os"
)

var clients = make([]*Client, 0)

type Client struct {
	Client   net.Conn
	Username string
}

type Payload struct {
	Message string `json:"message"`
	Code    string `json:"code"`
}

type Broadcast struct {
	Username string
	Payload  *Payload
}

func main() {
	reader := bufio.NewReader(os.Stdin)
	fmt.Print("Enter port: ")
	PORT, _ := reader.ReadString('\n')
	fmt.Println("Staring server on port: " + PORT)
	server, err := net.Listen("tcp", ":" + PORT)
	if server == nil {
		panic("Server error: " + err.Error())
	}
	lock := &sync.Mutex{}
	mainLoop(server, lock)
}

func mainLoop(server net.Listener, lock *sync.Mutex) {
	bMsgChan := make(chan *Broadcast)
	go broadcastMessageLoop(bMsgChan, lock)
	go broadcastUsernameLoop(lock)
	for {
		client, err := server.Accept()
		if client == nil {
			fmt.Println("Client connection error: " + err.Error())
			continue
		}
		lock.Lock()
		cStruct := &Client{
			Client:   client,
			Username: "",
		}
		clients = append(clients, cStruct)
		lock.Unlock()
		go handleClient(client, bMsgChan, lock, cStruct)
	}
}

func broadcastUsernameLoop(lock *sync.Mutex) {
	for {
		time.Sleep(time.Second * 5)
		data := ""
		lock.Lock()
		for _, cStruct := range clients {
			if strings.Compare(cStruct.Username, "") != 0 {
				data += cStruct.Username + ","
			}
		}
		if len(data) > 0 {
			data = data[:len(data)-1]
			payload, jsonErr := json.Marshal(map[string]string{"username": "update_usernames", "code": "2", "message": data})
			if jsonErr != nil {
				fmt.Println("JSON error:", jsonErr.Error())
				continue
			}
			for _, cStruct := range clients {
				_, err := cStruct.Client.Write(append(payload, []byte("\n")...))
				if err != nil {
					fmt.Println("Send error:", err.Error())
				}
			}
		}
		lock.Unlock()
	}
}

func broadcastMessageLoop(bMsgChan chan *Broadcast, lock *sync.Mutex) {
	for {
		payload := <-bMsgChan
		if strings.Compare(payload.Payload.Code, "0") == 0 {
			data, jsonErr := json.Marshal(map[string]string{"username": payload.Username, "code": payload.Payload.Code, "message": payload.Payload.Message})
			if jsonErr != nil {
				fmt.Println("JSON parse error: " + jsonErr.Error())
				continue
			}
			lock.Lock()
			for _, cStruct := range clients {
				_, err := cStruct.Client.Write(append(data, []byte("\n")...))
				if err != nil {
					fmt.Println("Send error:", err.Error())
				}
			}
			lock.Unlock()
		}
	}
}

func handleClient(client net.Conn, bMsgChan chan *Broadcast, lock *sync.Mutex, cStruct *Client) {
	defer func() {
		client.Close()
		lock.Lock()
		for i, cStruct := range clients {
			if cStruct.Client == client {
				clients = append(clients[:i], clients[i+1:]...)
				break
			}
		}
		lock.Unlock()
	}()
	clientAddr := client.RemoteAddr().String()[:strings.Index(client.RemoteAddr().String(), ":")]
	reader := bufio.NewReader(client)
	payload := new(Payload)
	username := ""
	fmt.Println("Client", clientAddr, "Connected")
	for {
		line, err := reader.ReadBytes('\n')
		trimmedLine := strings.Trim(string(line[:]), "\n")
		jsonErr := json.Unmarshal([]byte(trimmedLine), &payload)
		if err != nil {
			fmt.Println("Read error: " + err.Error())
			return
		} else if jsonErr != nil {
			fmt.Println("JSON parse error: " + jsonErr.Error())
			continue
		}

		fmt.Println("Addr:", clientAddr, "- Message:", payload.Message, "- Code:", payload.Code)

		//code 0 = broadcast chat message
		//code 1 = set client username
		//code 2 = broadcast all connected clients
		//code 3 = disconnect

		if strings.Compare(payload.Code, "0") == 0 {
			data := &Broadcast{
				Username: username,
				Payload:  payload,
			}
			bMsgChan <- data
		} else if strings.Compare(payload.Code, "1") == 0 {
			username = payload.Message
			lock.Lock()
			cStruct.Username = username
			lock.Unlock()
		} else if strings.Compare(payload.Code, "2") == 0 {
			data := &Broadcast{
				Username: username,
				Payload:  payload,
			}
			bMsgChan <- data
		} else if strings.Compare(payload.Code, "3") == 0 {
			client.Write([]byte("EXIT\n"))
			fmt.Println("Client", clientAddr, "Disconnected")
			return
		}
	}
}
