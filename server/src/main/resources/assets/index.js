const evtSource = new EventSource("/events");

evtSource.onmessage = function(e) {
  const data = JSON.parse(e.data);

  document.body.style.backgroundImage = "url('/img?path=" + data.first + "')";

  document.body.innerText = data.second.reduce((acc, o) => {
    return acc + "\n" + o.description + " = " + Math.round(o.score * 100) + "%";
  }, "");

};
