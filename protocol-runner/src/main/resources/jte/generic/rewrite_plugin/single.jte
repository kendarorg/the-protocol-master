@param org.kendar.plugins.apis.dtos.ReplacerItemFile data

<div id="singleFile">
    @if(data!=null)

        <div class="input-group">
            <button type="button" class="btn  btn-danger"
                    onclick="deleteContentFile()">
                Delete
            </button>
            &nbsp;
            <button type="button" class="btn  btn-success"
                    onclick="saveContentFile()">
                Update
            </button>
            &nbsp;
            <button type="button" class="btn  btn-success"
                    onclick="testMatching()">
                Test
            </button>
        </div>
        <br>

        <div class="input-group ">
            <label for="fileName" class="control-label">File Name</label>
            <div>
                <input id="fileName" type="text" readonly="readonly" class="form-control"
                       value="${data.getFileName()}"/>
            </div>
        </div>
        <br>
        <div class="input-group">
            <div class="checkbox">
                <label>
                    @if(data.getReplacerItem().isRegex())
                        <input id="regex" type="checkbox" checked>
                    @else
                        <input id="regex" type="checkbox">
                    @endif
                    Is regexp
                </label>
            </div>
        </div>
        <br>
        <div class="input-group">
            <label for="toFind" class="control-label">To Find</label>
            <div>
                <textarea id="toFind" rows="4" cols="200"
                          class="form-control">${data.getReplacerItem().getToFind()}</textarea>
            </div>
        </div>
        <br>
        <div class="input-group">
            <label for="toReplace" class="control-label">To Replace</label>
            <div>
                <textarea id="toReplace" rows="4" cols="200"
                          class="form-control">${data.getReplacerItem().getToReplace()}</textarea>
            </div>
        </div>
        <br>
        <div class="input-group">
            <label for="testTarget" class="control-label">To Verify</label>
            <div>
                <textarea id="testTarget" rows="4" cols="200" class="form-control"></textarea>
            </div>
        </div>
        <br>
    @endif
</div>