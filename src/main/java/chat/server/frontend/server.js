const socket = new WebSocket("ws://localhost:1871");

socket.addEventListener('open', (event) => {
    console.log('Connected to the Frontend interface');
});

socket.addEventListener('message', (event) => {
    console.log('Message: ', event.data);
    const message_window = document.getElementById("window-chat");
    console.log(event.data);

    const sender = event.data.split(" ")[0];

    if (sender == "") {return;}

    let msg_html = '<div class="message" id="';

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

socket.addEventListener('close', (event) => {
    if (event.wasClean) {
        console.log(`Connection closed cleanly, code=${event.code}, reason=${event.reason}`);
    } else {
        console.error(`Connection died`);
    }
});

socket.addEventListener('error', (error) => {
    console.error('WebSocket Error: ', error);
});

function send_message() {
    const message_window = document.getElementById("window-chat");
    let text = typing_input.value;
    socket.send(user_name + " " + text);
    typing_input.value = "";
    message_window.innerHTML += '<div class="message" id="sent">' + text + '<\div>';
    message_window.scrollTop = message_window.scrollHeight;
}
