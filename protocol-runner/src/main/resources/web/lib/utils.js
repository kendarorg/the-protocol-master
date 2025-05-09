// ===============================================
// XMLHTTPREQUEST UTILS
// ===============================================

function retrieveValue(id) {
    var el = document.getElementById(id);
    if (el.type === "number") {
        return +el.value;
    }else if (el.type === "checkbox") {
        return el.checked;
    }
    return el.value;
}

function setValue(id, value) {
    var el = document.getElementById(id);
    if (el.type === "number") {
        el.value = +value;
    } else if (el.type === "checkbox") {
        el.checked = value === true || value==="true" || value>0;
    } else {
        el.value = value;
    }

}

function retrieveValues(obj) {
    var propertyNames = Object.getOwnPropertyNames(obj);
    for (var i = 0; i < propertyNames.length; i++) {
        var id = propertyNames[i];
        try {

            var val = retrieveValue(id);
            obj[id] = val;
        } catch (error) {
            if (obj[id] === null) {
                console.warn("Missing id " + id);
            }
        }
    }
    return obj;
}

function sendData(path, verb, data, contentType, callback) {
    const xhr = new XMLHttpRequest();
    xhr.open(verb, path);

    // Send the proper header information along with the request
    xhr.setRequestHeader("Content-Type", contentType);

    xhr.onreadystatechange = () => {
        // Call a function when the state changes.
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (typeof callback === 'function') {
                if(xhr.status==200 || callback.length>=1) {
                    callback(xhr.status, xhr.response);
                }else if(xhr.status==200){
                    callback(xhr.status, xhr.response);
                }
            }
            if (xhr.status != 200) {
                console.error(xhr.status+" "+xhr.response);
                showNotification('error',"Error sending request",
                    "Path: "+path+
                    "\nContent Type: "+contentType+
                    "\nVerb: "+verb)
            }

            // Request finished. Do processing here.
        }
    };
    if (data != null) {
        return xhr.send(data);
    } else {
        return xhr.send();

    }
}

function getData(path, verb, callback) {
    const xhr = new XMLHttpRequest();
    xhr.open(verb, path);

    xhr.onreadystatechange = () => {
        // Call a function when the state changes.
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (typeof callback === 'function') {
                if(xhr.status==200 || callback.length>=1) {
                    callback(xhr.status, xhr.response);
                }else if(xhr.status==200){
                    callback(xhr.status, xhr.response);
                }
            }
            if (xhr.status != 200) {
                console.error(xhr.status+" "+xhr.response);
                showNotification('error',"Error sending no body request",
                    "Path: "+path+
                    "\nVerb: "+verb)
            }
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

    encodeParameters: function (xhr, parameters, elt) {
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

function prettifyJson(elementId) {
    var raw = retrieveValue(elementId);
    const obj = JSON.parse(raw);
    document.getElementById(elementId).value = JSON.stringify(obj, null, 2);
}

// ===============================================
// ACCORDION HANDLING
// ===============================================


function toggleAccordion(elementId) {
    var element = document.getElementById(elementId);
    if (element.classList.contains("in")) {
        console.log("CLOSE " + elementId)
        element.classList.remove("in")
        setAccordionStatus(elementId, "closed");
    } else {
        console.log("OPEN " + elementId)
        element.classList.add("in")
        setAccordionStatus(elementId, "open");
    }
}

function getAccordionStatus() {
    var item = localStorage.getItem("accordionStatus");
    if (item == null) {
        item = "{}";
    }
    return JSON.parse(item);
}

function setAccordionStatus(elementId, status) {
    var panelStatus = getAccordionStatus();
    panelStatus[window.location.pathname + "." + elementId] = {
        elementId: elementId,
        status: status,
        path: window.location.pathname
    };
    localStorage.setItem("accordionStatus", JSON.stringify(panelStatus));
}

function closeAccordion(elementId) {
    var element = document.getElementById(elementId);
    if (element.classList.contains("in")) {

        console.log("CLOSE " + elementId)
        element.classList.remove("in")
        setAccordionStatus(elementId, "closed");
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
            setAccordionStatus(elementId, "open");
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
            if (xhr.status != 200) {
                console.error(xhr.status+" "+xhr.response);
                showNotification('error',"Error uploading file",
                    "Path: "+path+
                    "\nContent Type: "+contentType+
                    "\nVerb: "+verb)
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
function openModal(id, path) {
    document.getElementById(id).classList.add('open');
    document.body.classList.add('jw-modal-open');
    htmx.ajax('GET', path, {target: "#" + id, swap: 'outerHTML'})
}

// close currently open modal
function closeModal() {
    document.querySelector('.jw-modal.open').classList.remove('open');
    document.body.classList.remove('jw-modal-open');
}

window.addEventListener('load', function () {
    // close modals on background click
    document.addEventListener('click', event => {
        if (event.target.classList.contains('jw-modal')) {
            closeModal();
        }
    });
});

function locationReload() {
    location.reload();
}

const NotificationSystem = {
    container: document.getElementById('notification-container'),
    timeout: 5000, // Default timeout 5 seconds

    init() {
        // Create container if it doesn't exist
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.className = 'notification-container';
            document.body.appendChild(this.container);
        }
    },

    show(type, title, message, timeout = this.timeout) {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;

        // Create content
        const content = document.createElement('div');
        content.className = 'notification-content';

        // Add title if provided
        if (title) {
            const titleElement = document.createElement('div');
            titleElement.className = 'notification-title';
            titleElement.textContent = title;
            content.appendChild(titleElement);
        }

        // Add message
        if(message!=null) {
            const messageElement = document.createElement('div');
            messageElement.className = 'notification-message';
            messageElement.textContent = message;
            content.appendChild(messageElement);
        }

        // Add close button
        const closeBtn = document.createElement('span');
        closeBtn.className = 'notification-close';
        closeBtn.innerHTML = '&times;';
        closeBtn.addEventListener('click', () => this.close(notification));

        // Assemble notification
        notification.appendChild(content);
        notification.appendChild(closeBtn);

        // Add to container
        this.container.appendChild(notification);

        // Set timeout for auto-close
        if (timeout > 0) {
            notification.timeoutId = setTimeout(() => {
                this.close(notification);
            }, timeout);
        }

        // Return notification element for potential further manipulation
        return notification;
    },

    close(notification) {
        // Clear timeout if exists
        if (notification.timeoutId) {
            clearTimeout(notification.timeoutId);
        }

        // Add fade-out animation
        notification.classList.add('fade-out');

        // Remove after animation completes
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 500); // Match duration of fadeOut animation
    },

    // Shorthand methods for common notification types
    success(title, message, timeout) {
        return this.show('success', title, message, timeout);
    },

    error(title, message, timeout) {
        return this.show('error', title, message, timeout);
    },

    info(title, message, timeout) {
        return this.show('info', title, message, timeout);
    },

    warning(title, message, timeout) {
        return this.show('warning', title, message, timeout);
    }
};

function showNotification(type, title, message) {
    NotificationSystem.show(type, title, message);
}

function showAlert(title) {
    NotificationSystem.show("info", "INFO", title);
}

function showError(title) {
    NotificationSystem.show("error", "ERROR", title);
}

function handleHtmxResults(){
    document.addEventListener('htmx:afterRequest', function(evt) {
        if(evt.detail.xhr.status == 404){
            /* Notify the user of a 404 Not Found response */
            return showError("Error: Could Not Find Resource");
        }
        if (evt.detail.successful != true) {
            /* Notify of an unexpected error, & print error to console */
            showNotification("error","Unexpected Error",evt);
            return console.error(evt);
        }
    });
}

