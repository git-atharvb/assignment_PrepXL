const canvas = document.getElementById("visualizer");
const ctx = canvas.getContext("2d");
const startBtn = document.getElementById("startBtn");
const stopBtn = document.getElementById("stopBtn");
const pauseBtn = document.getElementById("pauseBtn");
const gainSlider = document.getElementById("gain");
const fftSelect = document.getElementById("fft");
const rotSlider = document.getElementById("rotation");
const colorBtn = document.getElementById("colorBtn");

let audioCtx, analyser, source, stream;
let dataArray;
let running = false;
let paused = false;
let mono = false;
let rotation = 0;

function resizeCanvas() {
  canvas.width = canvas.offsetWidth;
  canvas.height = canvas.offsetHeight;
}

resizeCanvas();
window.addEventListener("resize", resizeCanvas);
startBtn.onclick = async () => {
  if (running) return;
  audioCtx = new AudioContext();
  analyser = audioCtx.createAnalyser();
  analyser.fftSize = +fftSelect.value;
  analyser.smoothingTimeConstant = 0.85;
  stream = await navigator.mediaDevices.getUserMedia({ audio: true });
  source = audioCtx.createMediaStreamSource(stream);
  source.connect(analyser);
  dataArray = new Uint8Array(analyser.frequencyBinCount);
  running = true;
  paused = false;
  startBtn.disabled = true;
  stopBtn.disabled = false;
  pauseBtn.disabled = false;
  pauseBtn.textContent = "Pause Visual";
  animate();
};

stopBtn.onclick = () => {
  running = false;
  paused = false;
  if (stream) stream.getTracks().forEach(t => t.stop());
  if (audioCtx) audioCtx.close();
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  startBtn.disabled = false;
  stopBtn.disabled = true;
  pauseBtn.disabled = true;
};

pauseBtn.onclick = () => {
  paused = !paused;
  pauseBtn.textContent = paused ? "Resume Visual" : "Pause Visual";
};

fftSelect.onchange = () => {
  if (!analyser) return;
  analyser.fftSize = +fftSelect.value;
  dataArray = new Uint8Array(analyser.frequencyBinCount);
};

colorBtn.onclick = () => {
  mono = !mono;
  colorBtn.textContent = mono ? "Mono Mode" : "Spectrum Mode";
};

function animate() {
  if (!running) return;
  requestAnimationFrame(animate);
  if (paused) return;
  analyser.getByteFrequencyData(dataArray);
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  const cx = canvas.width / 2;
  const cy = canvas.height / 2;
  const radius = Math.min(cx, cy) - 60;
  rotation += +rotSlider.value;
  for (let i = 0; i < dataArray.length; i++) {
    const value = dataArray[i] * gainSlider.value;
    const angle = (i / dataArray.length) * Math.PI * 2 + rotation;
    const x1 = cx + Math.cos(angle) * radius;
    const y1 = cy + Math.sin(angle) * radius;
    const x2 = cx + Math.cos(angle) * (radius + value);
    const y2 = cy + Math.sin(angle) * (radius + value);
    ctx.strokeStyle = mono
      ? "#0ff"
      : `hsl(${i * 360 / dataArray.length}, 100%, 60%)`;
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.stroke();   
  }
}