function sendData(path, verb, data, contentType, callback) {
    const xhr = new XMLHttpRequest();
    xhr.open(verb, path);

    // Send the proper header information along with the request
    xhr.setRequestHeader("Content-Type", contentType);

    xhr.onreadystatechange = () => {
        // Call a function when the state changes.
        if (xhr.readyState === XMLHttpRequest.DONE) {
            callback(xhr.status, xhr.response);
            // Request finished. Do processing here.
        }
    };
    xhr.send(data);
}

function getData(path, verb, callback) {
    const xhr = new XMLHttpRequest();
    xhr.open(verb, path);

    xhr.onreadystatechange = () => {
        // Call a function when the state changes.
        if (xhr.readyState === XMLHttpRequest.DONE) {
            callback(xhr.status, xhr.response);
            // Request finished. Do processing here.
        }
    };
    xhr.send();
}


function deepClone(obj) {
    return JSON.parse(JSON.stringify(obj));
}

const params = new Proxy(new URLSearchParams(window.location.search), {
    get: (searchParams, prop) => searchParams.get(prop),
});

htmx.defineExtension('submitjson', {
    onEvent: function (name, evt) {
        if (name === "htmx:configRequest") {
            evt.detail.headers['Content-Type'] = "application/json"
            evt.detail.headers['X-API-Key'] = 'sjk_xxx'
        }
    },
    encodeParameters: function (xhr, parameters, elt) {
        xhr.overrideMimeType('text/json') // override default mime type
        return (JSON.stringify(parameters))
    }
})



function toggleAccordion(elementId){
    var element = document.getElementById(elementId);
    if (element.classList.contains("in")) {
        console.log("CLOSE")
        element.classList.remove("in")
    } else {
        console.log("OPEN")
        element.classList.add("in")
    }
}

function isAccordionOpen(elementId){
    var element = document.getElementById(elementId);
    return element.classList.contains("in");
}

function openAccordion(elementId){
    console.log("OPEN "+elementId)
    setTimeout(function() {
        var element = document.getElementById(elementId);
        if (!element.classList.contains("in")) {
            console.log("OPEN")
            element.classList.add("in")
        }
    }, 250);
    //debugger;
    //htmx.toggleClass(htmx.find("#elementId"), "in");

}