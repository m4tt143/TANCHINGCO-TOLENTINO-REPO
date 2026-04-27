
const correctUser = "Mikael";
const correctPass = "perps2026";

// audio for wrong password
const beep = new Audio('mixkit-shop-scanner-beeps-1073.wav');

let lastUsername = "";
let lastTime = "";

function login() {
    let user = document.getElementById("username").value;
    let pass = document.getElementById("password").value;

    if (user === correctUser && pass === correctPass) {
    
        let now = new Date();
        lastTime = now.toLocaleString();
        lastUsername = user;

        document.getElementById("message").innerText = "Welcome, " + user + "!";
        document.getElementById("time").innerText = "Logged in at: " + lastTime;

        document.getElementById("downloadBtn").style.display = "inline";
    } else {

        document.getElementById("message").innerText = "Incorrect login!";
        beep.play();
        document.getElementById("downloadBtn").style.display = "none";
    }
}

function downloadAttendance() {
    let text = "Username: " + lastUsername + "\nTimestamp: " + lastTime;

    const blob = new Blob([text], { type: "text/plain" });
    const link = document.createElement("a");

    link.href = URL.createObjectURL(blob);
    link.download = "attendance_summary.txt";
    link.click();
}
