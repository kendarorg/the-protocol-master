// ===============================================
// XMLHTTPREQUEST UTILS
// ===============================================

function sendData(path, verb, data, contentType, callback) {
    const xhr = new XMLHttpRequest();
    xhr.open(verb, path);

    // Send the proper header information along with the request
    xhr.setRequestHeader("Content-Type", contentType);

    xhr.onreadystatechange = () => {
        // Call a function when the state changes.
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if(typeof callback === 'function')callback(xhr.status, xhr.response);
            // Request finished. Do processing here.
        }
    };
    return xhr.send(data);
}

function getData(path, verb, callback) {
    const xhr = new XMLHttpRequest();
    xhr.open(verb, path);

    xhr.onreadystatechange = () => {
        // Call a function when the state changes.
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if(typeof callback === 'function')callback(xhr.status, xhr.response);
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
/*
htmx.defineExtension('submitjson', {
    onEvent: function (name, evt) {
        if (name === "htmx:configRequest") {
            evt.detail.headers['Content-Type'] = "application/json"
        }
    },
    encodeParameters: function (xhr, parameters, elt) {
        xhr.overrideMimeType('application/json') // override default mime type
        return (JSON.stringify(parameters))
    }
})*/

htmx.defineExtension('json-enc', {
    onEvent: function (name, evt) {
        if (name === "htmx:configRequest") {
            evt.detail.headers['Content-Type'] = "application/json";
        }
    },

    encodeParameters : function(xhr, parameters, elt) {
        xhr.overrideMimeType('text/json');
        return (JSON.stringify(parameters));
    }
});

function downloadURI(uri, name) {
    var link = document.createElement("a");
    try {
        if (typeof name !== 'undefined' && name !== null) {
            link.download = name; // <- name instead of 'name'
        }
        link.href = uri;
        link.click();
    } catch (err) {
        console.log(err)
    }
    link.remove();
// <------------------------------------------       Do something (show loading)
//     fetch(uri)
//         .then(resp => resp.blob())
//         .then(blob => {
//             const url = window.URL.createObjectURL(blob);
//             const a = document.createElement('a');
//             a.style.display = 'none';
//             a.href = url;
//             // the filename you want
//             a.download = name;
//             document.body.appendChild(a);
//             a.click();
//             window.URL.revokeObjectURL(url);
//             // <----------------------------------------  Detect here (hide loading)
//             alert('File detected');
//             a.remove(); // remove element
//         })
//         .catch(() => alert('An error sorry'));
}

// ===============================================
// ACCORDION HANDLING
// ===============================================

function toggleAccordion(elementId) {
    var element = document.getElementById(elementId);
    if (element.classList.contains("in")) {
        console.log("CLOSE")
        element.classList.remove("in")
    } else {
        console.log("OPEN")
        element.classList.add("in")
    }
}

function isAccordionOpen(elementId) {
    var element = document.getElementById(elementId);
    return element.classList.contains("in");
}

function openAccordion(elementId) {
    console.log("OPEN " + elementId)
    setTimeout(function () {
        var element = document.getElementById(elementId);
        if (!element.classList.contains("in")) {
            console.log("OPEN")
            element.classList.add("in")
        }
    }, 250);
    //debugger;
    //htmx.toggleClass(htmx.find("#elementId"), "in");

}


// ===============================================
// UPLOAD DOWNLOAD
// ===============================================

// add a function to call when the <input type=file> status changes, but don't "submit" the form
//document.getElementById('upload').addEventListener('change', handle_file_select, false);

let currentFileData = null;

function send_file(path, data, contentType, output, outputCode) {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", path);

// Send the proper header information along with the request
    xhr.setRequestHeader("Content-Type", contentType);

    xhr.onreadystatechange = () => {
        // Call a function when the state changes.
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (typeof outputCode !== 'undefined' && outputCode !== null) {
                outputCode.innerHTML = xhr.status;
            }
            if (typeof output !== 'undefined' && output !== null) {
                output.innerHTML = xhr.response;
            }
            // Request finished. Do processing here.
        }
    };
    xhr.send(data);
}

function async_file_clean(item) {
    var target = document.getElementById(item);
    let outputPath = find_child_within_tree(target, "#" + item + "File")
    if (typeof outputPath !== 'undefined' && outputPath != null) {
        outputPath.value = "";
    }
    currentFileData = null;
}

function async_file_send() {
    let evt = currentFileData;
    const params = new Proxy(new URLSearchParams(window.location.search), {
        get: (searchParams, prop) => searchParams.get(prop),
    });
// Get the value of "some_key" in eg "https://example.com/?some_key=some_value"
    let path = params.path; // "some_value"
    if (typeof path === 'undefined' || path === null) {
        path = evt.target.getAttribute("path");
    }
    let contentType = params.contentType;
    if (typeof contentType === 'undefined' || contentType === null) {
        contentType = evt.target.getAttribute("contentType");
    }

    //let path = parent.querySelector("#"+evt.currentTarget.id+"Path").value;
    let binary = params.binary;
    if (typeof binary === 'undefined' || binary === null) {
        binary = evt.target.getAttribute("binary");
    }
    if (typeof binary === 'undefined' || binary === null) {
        binary = true;
    }
    if (typeof contentType === 'undefined' || contentType === null) {
        if (binary) {
            contentType = "application/octet-stream"
        } else {
            contentType = "text/plain"
        }
    }

    let output = find_child_within_tree(evt.target, "#" + evt.target.id + "Output")
    let outputCode = find_child_within_tree(evt.target, "#" + evt.target.id + "Code")
    let fl_files = evt.target.files; // JS FileList object

    // use the 1st file from the list
    let fl_file = fl_files[0];

    let reader = new FileReader(); // built in API

    let display_file = (e) => { // set the contents of the <textarea>
        console.info('. . got: ', e.target.result.length, e);
        //document.getElementById( 'upload_file' ).innerHTML = e.target.result;
        send_file(path, e.target.result, contentType, output, outputCode)
    };

    let on_reader_load = (fl) => {
        console.info('. file reader load', fl);
        return display_file; // a function
    };

    // Closure to capture the file information.
    reader.onload = on_reader_load(fl_file);

    // Read the file as text.
    if (binary) {
        reader.readAsArrayBuffer(fl_file);
    } else {
        reader.readAsText(fl_file);
    }
}

function find_child_within_tree(item, selector) {
    let outputPath = null;
    var parent = item.parentNode;
    for (var i = 0; i < 10; i++) {
        outputPath = parent.querySelector(selector);
        if (typeof outputPath === 'undefined' || outputPath === null) {
            parent = parent.parentNode;
        } else {
            break;
        }
    }
    return outputPath;
}

function handle_file_select(evt) {
    console.info("[Event] file chooser");
    let outputPath = find_child_within_tree(evt.target, "#" + evt.target.id + "File")

    if (typeof outputPath !== 'undefined' && outputPath != null) {
        outputPath.value = evt.target.value;
        currentFileData = evt;
    } else {
        async_file_send();
    }
}

// =========================================
// MODAL
// =========================================


// open modal by id
function openModal(id,path) {
    document.getElementById(id).classList.add('open');
    document.body.classList.add('jw-modal-open');
    htmx.ajax('GET', path, {target:"#"+id, swap:'outerHTML'})
}

// close currently open modal
function closeModal() {
    document.querySelector('.jw-modal.open').classList.remove('open');
    document.body.classList.remove('jw-modal-open');
}

window.addEventListener('load', function() {
    // close modals on background click
    document.addEventListener('click', event => {
        if (event.target.classList.contains('jw-modal')) {
            closeModal();
        }
    });
});
