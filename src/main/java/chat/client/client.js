const socket = new WebSocket("ws://localhost:1870");
const typing_input = document.getElementById("composer");
const typing_button = document.getElementById("send");
let user_name = "PLACEHOLDER";
let logged_in = false;
var databuffer = "";

// Add an event listener to the input field for the "keydown" event
typing_input.addEventListener("keydown", function(event) {
    // Check if the pressed key is the Enter key (keycode 13)
    if (event.keyCode == 13) {
        // Trigger a click event on the button
        typing_button.click();
    }
});

window.addEventListener('beforeunload', function(event) {
    send("CLOSE");
});

// Connection opened
socket.addEventListener("open", (event) => {
    console.log("Connected to the server");
});

// Listen for messages from the server
socket.addEventListener("message", (event) => {
    console.log(event.data);
    databuffer += event.data.substring(6);
    if (event.data.split("<|>")[0] == "END") {
        processmsg(databuffer);
        databuffer = "";
    }
});

function processmsg(msg) {
    const message_window = document.getElementById("window-chat");
    let sender = msg.split("<|>")[0];
    let status = msg.split("<|>")[1];

    if (sender == "ROOMS") {
        update_rooms(status);
        return;
    }
    else if (sender == "CONNECTED") {
        update_connected(status);
        return;
    }
    else if (sender == "LOGIN") {
        if (status == "SUCCESS") {
            hide_login_window();
            typing_input.placeholder = "Schreiben als " + user_name + "..."
            logged_in = true;
        }
        else if (status == "WRONG") {
            alert("Passwort oder Name stimmen nicht.")
        }
        else if (status == "BANNED") {
            alert("Dieser Account wurde gesperrt.")
        }
        else if (status == "DUPLICATE") {
            alert("Ein Account mit diesem Name ist bereits registriert!")
        }
        else {
            alert("Login Fehler " + status);
        }
        return;
    }

    if (sender == "" || !logged_in) {return;}
    let msg_html = '<div class="message" id="';
    if (sender == "SERVER") {
        msg_html += 'server">';
    }
    else {
        msg_html += 'recieved"><h3>' + sender + '</h3>';
    }
    if (status == "!IMG") {
        // IMAGE RECIEVING
        let imageData = msg.split("<|>")[2];
        msg_html += "<img alt='sent img' src='";
        msg_html += imageData;
        msg_html += "'>"
    }
    else if (status == "!PDF") {
        // PDF RECIEVING
        let pdfData = msg.split("<|>")[2];
        msg_html += '<object type="application/pdf" height="300px" data="';
        msg_html += pdfData + '"></object>';
    }
    else {
        msg_html += status;
    }
    msg_html += "</div>";
    message_window.innerHTML += msg_html;

    message_window.scrollTop = message_window.scrollHeight;
}

// Connection closed
socket.addEventListener("close", (event) => {
    alert("No server connection. Retrying...");
    window.location.reload();
});

// Connection error
socket.addEventListener("error", (event) => {
    console.error("Error occurred:", event);
});

function send_message() {
    // Send text first
    const message_window = document.getElementById("window-chat");
    let text = typing_input.value;
    if (text != "") {
        typing_input.value = "";
        send(user_name + "<|>" + text);
        message_window.innerHTML += '<div class="message" id="sent">' + text + '<\div>';
        message_window.scrollTop = message_window.scrollHeight;
    }

    // Send file after
    const filein = document.getElementById("file");
    const file = filein.files[0];
    if (file === undefined) { return; }
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onloadend = function (e) {

        // IMAGE SENDING
        if (!file.name.endsWith(".pdf")) {
            send(user_name + "<|>" + "!IMG" + "<|>" + e.target.result);
            let addhtml = '<div class="message" id="sent"><img src="';
            addhtml += e.target.result + '" alt="sent_img"><\div>';
            message_window.innerHTML += addhtml;
        }
        else {
            // PDF SENDING
            send(user_name + "<|>" + "!PDF" + "<|>" + e.target.result);
            addhtml = '<div class="message" id="sent"><object type="application/pdf" height="300px" data="';
            addhtml += e.target.result + '"><\div>';
            message_window.innerHTML += addhtml;
            message_window.scrollTop = message_window.scrollHeight;
        }
    }
    filein.value = "";
}

function login() {
    let name = document.getElementById("name").value;
    let pw = document.getElementById("pw").value;
    if (name.includes("<|>") || pw.includes("<|>")) {
        alert("<|> is not allowed.");
    }
    if (name.length + pw.length < 7) {
        alert("Name und Passwort müssen jeweils mindestens 3 Zeichen lang sein.")
    }
    send("LOGIN<|>" + name + "<|>" + pw);
    user_name = name;
}

function register() {
    let pwc = document.getElementById("passconfirm");
    if (pwc.style.display == "") {
        pwc.style.display = "block";
        return;
    }
    let pw = document.getElementById("pw");
    if (pwc.value != pw.value) {
        alert("Passwörter stimmen nicht überein!");
        pwc.value = "";
        return;
    }
    let name = document.getElementById("name");
    if (name.value.length + pw.value.length < 7) {
        alert("Name und Passwort müssen jeweils mindestens 3 Zeichen lang sein.")
        return;
    }
    if (name.value.includes("|") || name.value.includes(",") || name.value.includes("/!!/") || name.value.includes("@") || pw.value.includes("<|>")) {
        // <|> ist Trennzeichen für Protokoll
        // /!!/ ist Markierung für gesperrte Accounts
        // , Frontend Trennzeichen für verbundene Clients
        // @ ist Bezeichnung für aktiven Raum
        alert("'|', '/!!/', '@' und ',' sind nicht im name oder im Passwort erlaubt.");
        return;
    }
    if(name.value == "SERVER" || name.value == "LOGIN" || name.value == "REGISTER" || name.value == "COMMAND"){
        alert("Name ist reserviert und kann nicht verwendet werden.")
        return;
    }
    send("REGISTER<|>" + name.value + "<|>" + pw.value);
    user_name = name.value;
}

function hide_login_window() {
    document.getElementById("login").style.display = "none";
}

function update_rooms(roominfo) {
    const rooms = roominfo.split("|");
    const roomlist = document.getElementById("room-container");
    let newhtml = "<i>Offene Chats</i>";
    for (const i in rooms) {
        const roomname = rooms[i].split("/!!/")[0].split("@")[1];
        const roompop = rooms[i].split("/!!/")[0].split("@")[0];
        newhtml += '<button class="roomcard" '
        if (rooms[i].includes("/!!/")) {
            newhtml += 'id="activeroom" ';
        }
        newhtml += 'onclick="switch_to_room(\'';
        newhtml += roomname;
        newhtml += '\')"><i>[' + roompop + ']</i> ';
        newhtml += roomname;
        newhtml += '</button>'
    }
    roomlist.innerHTML = newhtml;
}

function switch_to_room(room_name) {
    console.log("Switching to room " + room_name);
    const message_window = document.getElementById("window-chat");
    message_window.innerHTML = "";
    send("SWITCHROOM<|>" + room_name);
}

function update_connected(connectedinfo) {
    const roomlist = document.getElementById("connected-container");
    let newhtml;
    if (connectedinfo.length > 0) {
        newhtml = "<i>Verbundene Nutzer</i>";
        const users = connectedinfo.split("|");
        for (const i in users) {
            const username = users[i];
            newhtml += '<div>' + username + '</div>';
        }
    }
    else {
        newhtml = "<i>Noch ist keiner hier...</i>"
    }
    roomlist.innerHTML = newhtml;
}

function send(data) {
    const chunksize = 65000;
    datasplit = splitString(data, chunksize);
    for (let i = 0; i < datasplit.length - 1; i++) {
        socket.send("DAT<|>" + datasplit[i]);
    }
    socket.send("END<|>" + datasplit[datasplit.length-1])
}

function splitString(inputString, chunkSize) {
    const regex = new RegExp(`.{1,${chunkSize}}`, 'g');
    return inputString.match(regex) || [];
}
