const socket = new WebSocket("ws://localhost:1871");
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

socket.addEventListener('open', (event) => {
    console.log('Connected to the Frontend interface');
});

socket.addEventListener('message', (event) => {
    console.log('Message: ', event.data);
    const message_window = document.getElementById("window-chat");

    const sender = event.data.split("<|>")[0];

    if (sender == "REGISTERED") {
        console.log(event.data.split("<|>")[1]);
        update_registered(event.data.split("<|>")[1]);
        return;
    }

    if (sender == "") {return;}

    let msg_html = '<div class="message" id="';

    if (sender == "SERVER") {
        msg_html += 'server">';
    }
    else {
        msg_html += 'recieved"><h3>' + sender + '</h3>';
    }
    msg_html += event.data.split("<|>")[1];
    msg_html += "</div>";
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
    let text = typing_input.value;
    socket.send("SERVER<|>" + text);
    typing_input.value = "";
}

function stop_server() {
    socket.send("CONTROL<|>EXIT");
}

function update_registered(data) {
    const window = document.getElementById("registeredList");
    window.innerHTML = "";
    let names = data.split("|");

    let target = '<div class="nameline"><div>Name</div><button onclick="banUser("name")" class="secondary">Ban</button></div>';
    console.log(names);
    for (let i = 0; i < names.length-1; i++) {
        let addhtml = '<div class="nameline"><div>';
        addhtml += names[i].replace("/!!/", "");
        if (!names[i].includes("/!!/")) { // User blocked flag
            addhtml += '</div><button onclick="banUser(\'';
        }
        else {
            addhtml += '</div><button onclick="unbanUser(\'';
        }
        addhtml += names[i].replace("/!!/", "");
        if (!names[i].includes("/!!/")) { // User blocked flag
            addhtml += '\')" class="secondary">Ban</button></div>';
        }
        else {
            addhtml += '\')" class="secondary">Unban</button></div>';
        }
        window.innerHTML += addhtml;
    }
}

function banUser(name) {
    socket.send("CONTROL<|>BAN " + name);
}

function unbanUser(name) {
    socket.send("CONTROL<|>UNBAN " + name);
}
