function poll() {

  fetch("/events").then(res => {
    if (res.ok) {
      res.json().then(data => {
        document.body.style.backgroundImage = "url('/img?path=" + data.first + "')";

        document.body.innerText = data.second.reduce((acc, o) => {
          return acc + "\n" + o.description + " = " + Math.round(o.score * 100) + "%";
        }, "");
      });
    }
  });

}

window.setInterval(poll, 1000);
