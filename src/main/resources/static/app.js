const video = document.getElementById('video');
const canvas = document.getElementById('canvas');
const result = document.getElementById('result');

// Start webcam
navigator.mediaDevices.getUserMedia({ video: true })
  .then(stream => {
    video.srcObject = stream;
  })
  .catch(err => {
    console.error("Webcam error:", err);
  });

// Capture snapshot from video
function getSnapshotBlob() {
  const ctx = canvas.getContext('2d');
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

  return new Promise(resolve => {
    canvas.toBlob(blob => {
      resolve(blob);
    }, 'image/jpeg');
  });
}

// Enroll face with name
async function enrollFace() {
  const name = document.getElementById('nameInput').value.trim();
  if (!name) {
    alert('Please enter a name for enrollment.');
    return;
  }

  const blob = await getSnapshotBlob();
  const formData = new FormData();
  formData.append("name", name);
  formData.append("file", blob, "snapshot.jpg");

  const res = await fetch("http://localhost:8080/enroll", {
    method: "POST",
    body: formData
  });

  const json = await res.json();
  result.innerText = `Enrolled: ${json.name}`;
}

// Recognize face
async function recognizeFace() {
  const blob = await getSnapshotBlob();
  const formData = new FormData();
  formData.append("file", blob, "snapshot.jpg");

  const res = await fetch("http://localhost:8080/recognize", {
    method: "POST",
    body: formData
  });

  const json = await res.json();
  result.innerText = `Recognized: ${json.name}`;

  // Draw face rectangle + label
  drawFaceRect(json.rect, json.name);
}

const overlay = document.getElementById('overlay');
const ctx = overlay.getContext('2d');

function drawFaceRect(rect, name) {
  ctx.clearRect(0, 0, overlay.width, overlay.height);
  if (!rect) return;

  ctx.strokeStyle = 'lime'; // green
  ctx.lineWidth = 3;
  ctx.font = "16px Arial";
  ctx.fillStyle = 'lime';

  ctx.strokeRect(rect.x, rect.y, rect.width, rect.height);

  const text = name || 'unidentified';
  ctx.fillText(text, rect.x, rect.y > 20 ? rect.y - 5 : rect.y + 15);
}
