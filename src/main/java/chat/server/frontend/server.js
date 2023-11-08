const socket = new WebSocket("ws://localhost:1871");
const typing_input = document.getElementById("composer");
const typing_button = document.getElementById("send");

window.addEventListener('beforeunload', function(event) {
    alert("Schließen des Frontends fährt den server runter.");
    stop_server();
    socket.close();
});

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
        update_registered(event.data.split("<|>")[1]);
        return;
    }
    else if (sender == "CONNECTED") {
        update_connected(event.data.split("<|>")[1]);
        return;
    }
    else if (sender == "ROOMS") {
        update_rooms(event.data.split("<|>")[1]);
        return;
    }

    if (sender == "") {return;}

    let msg_html = '<div class="message" id="';

    const user = event.data.split("<|>")[1]
    if (user == "") {
        return;
    }
    if (user == "SERVER") {
        msg_html += 'server"><i>@ ' + sender + '</i><br>';
    }
    else {
        msg_html += 'recieved"><h3>' + user + ' @' + sender + '</h3>';
    }

    msg_html += event.data.split("<|>")[2];
    msg_html += "</div>";
    message_window.innerHTML += msg_html;

    message_window.scrollTop = message_window.scrollHeight;
});

socket.addEventListener('close', (event) => {
    alert("Server wurde herruntergefahren. Dieses Fenster schließt sich jetzt.");
    window.location.href = "about:blank";
    window.close();
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

    for (let i = 0; i < names.length-1; i++) {
        let addhtml = '<div class="nameline"><div>';
        // Add name
        addhtml += names[i].replace("/!!/", "");
        addhtml += "</div><div>";


        // Build block button
        if (!names[i].includes("/!!/")) { // User blocked flag
            addhtml += '<button onclick="banUser(\'';
        }
        else {
            addhtml += '<button onclick="unbanUser(\'';
        }
        addhtml += names[i].replace("/!!/", "");
        if (!names[i].includes("/!!/")) { // User blocked flag
            addhtml += '\')" class="secondary">Ban</button>';
        }
        else {
            addhtml += '\')" class="secondary">Unban</button>';
        }

        // Build delete Button
        addhtml += '<button style="color:red" onclick="deleteUser(\'';
        addhtml += names[i].replace("/!!/", "");
        addhtml += '\')" class="secondary">Delete</button>';

        addhtml += '</div></div>';
        window.innerHTML += addhtml;
    }
}

function banUser(name) {
    socket.send("CONTROL<|>BAN " + name);
}

function deleteUser(name) {
    socket.send("CONTROL<|>DELETE " + name);
}

function unbanUser(name) {
    socket.send("CONTROL<|>UNBAN " + name);
}

function update_connected(data) {
    const window = document.getElementById("connectedList");
    window.innerHTML = "";
    if (data == "") {
        return;
    }
    let names = data.split("|");

    for (let i = 0; i < names.length; i++) {
        const name = names[i].split("@")[0];
        const room = names[i].split("@")[1];
        let addhtml = '<div class="nameline"><div>';
        addhtml += name;
        addhtml += " <i>@" + room + "</i>";
        addhtml += '</div><button onclick="kickUser(\'';
        addhtml += name;
        addhtml += '\')" class="secondary">Kick</button></div>';
        window.innerHTML += addhtml;
    }
}

function kickUser(name) {
    socket.send("CONTROL<|>KICK " + name);
    console.log("KICKED");
}

function update_rooms(data) {
    const window = document.getElementById("roomList");
    window.innerHTML = "";

    let rooms = data.split("|");

    for (let i = 0; i < rooms.length; i++) {
        const count = rooms[i].split("@")[0];
        const roomname = rooms[i].split("@")[1];
        let addhtml = '<div class="nameline"><div>';
        addhtml += "<i>[" + count + "]</i> <input type='text' onchange='renameroom(\"" + roomname + "\", this.value)' value='";
        addhtml += roomname;
        addhtml += '\'></div>';
        // Delete Button
        addhtml += '<button onclick="deleteroom(\'';
        addhtml += roomname;
        addhtml += '\')" class="secondary" style="color:red;">Delete</button></div>';
        window.innerHTML += addhtml;
    }
}

function createroom() {
    const nameinput = document.getElementById("newroomname");
    socket.send("CONTROL<|>CREATEROOM " + nameinput.value);
    nameinput.value = "";
}

function deleteroom(roomname) {
    socket.send("CONTROL<|>DELETEROOM " + roomname);
}

function renameroom(oldname, newname) {
    socket.send("CONTROL<|>RENAMEROOM " + oldname + " " + newname);
}
