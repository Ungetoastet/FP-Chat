const socket = new WebSocket("ws://localhost:1870");

// Connection opened
socket.addEventListener("open", (event) => {
    console.log("Connected to the server");
    // Send data to the server
    socket.send("Hello wassuuuuuup!");
});

// Listen for messages from the server
socket.addEventListener("message", (event) => {
    console.log("Message from server:", event.data);
});

// Connection closed
socket.addEventListener("close", (event) => {
    if (event.wasClean) {
        console.log("Connection closed cleanly, code=" + event.code + ", reason=" + event.reason);
    } else {
        console.error("Connection abruptly closed");
    }
});

// Connection error
socket.addEventListener("error", (event) => {
    console.error("Error occurred:", event);
});
