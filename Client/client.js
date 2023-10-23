const socket = new WebSocket("ws://localhost:1870");
const typing_input = document.getElementById("composer");
const typing_button = document.getElementById("send");

// Add an event listener to the input field for the "keydown" event
typing_input.addEventListener("keydown", function(event) {
    // Check if the pressed key is the Enter key (keycode 13)
    if (event.keyCode == 13) {
        // Trigger a click event on the button
        typing_button.click();
    }
});


// Connection opened
socket.addEventListener("open", (event) => {
    console.log("Connected to the server");
});

// Listen for messages from the server
socket.addEventListener("message", (event) => {
    var message_window = document.getElementById("window-chat");

    var sender = event.data.split(" ")[0];
    if (sender == "") {return;}
    var msg_html = '<div class="message" id="';
    if (sender == "SERVER") {
        msg_html += 'server">';
    }
    else {
        msg_html += 'recieved"><h3>' + sender + '</h3>';
    }
    msg_html += event.data.split(' ').slice(1).join(' '); // Erstes Wort abschneiden
    msg_html += "</div>"
    message_window.innerHTML += msg_html;  

    message_window.scrollTop = message_window.scrollHeight;
    console.log("Message from server:", event.data);
});

// Connection closed
socket.addEventListener("close", (event) => {
    if (event.wasClean) {
        console.log("Connection closed cleanly, code=" + event.code + ", reason=" + event.reason);
    } else {
        console.error("Connection abruptly closed");
    }
    alert("No server connection.");
});

// Connection error
socket.addEventListener("error", (event) => {
    console.error("Error occurred:", event);
});

function send_message() {
    var message_window = document.getElementById("window-chat");
    text = typing_input.value;
    socket.send("NAMEPLACEHOLDER " + text);
    typing_input.value = "";
    message_window.innerHTML += '<div class="message" id="sent">' + text + '<\div>';
    message_window.scrollTop = message_window.scrollHeight;
}
