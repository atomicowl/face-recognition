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
}
