/** @jsx React.DOM */
var SpringBootAppConfig = React.createClass({
    getInitialState: function() {
        return {selectedSpringBootApp: null, showScreenOverlay: false};
    },
    render: function() {
        return <div className="SpringBootAppConfig">
                   <ScreenOverlay show={this.state.showScreenOverlay} />
                   <div className="btnContainer">
                       <GenericButton label="Delete" accessKey="d" callback={this.delBtnCallback}/>
                       <GenericButton label="Add" accessKey="a" callback={this.addBtnCallback}/>
                   </div>

                   <RDataTable ref="dataTable"
                               tableIndex="id"
                               colDefinitions={[{key: "id", isVisible: false},
                                                {title: "Name", key: "name", renderCallback: this.springBootAppNameRenderCallback},
                                                {title: "Hosts", key: "hostNames"},
                                                {title: "Jar or War File", key: "archiveFilename"},
                                                {title: "GitHub Link", key: "gitHubLink"},
                                                {title: "JDK", key: "jdkMedia.name"}]}
                               selectItemCallback={this.selectItemCallback}
                               deselectAllRowsCallback={this.deselectAllRowsCallback}/>

                   <ModalDialogBox ref="modalAddSpringBootAppDlg"
                                   title="Add SpringBoot App"
                                   okCallback={this.okAddCallback}
                                   content={<SpringBootAppConfigForm ref="springBootAppAddForm"/>}/>

                   <ModalDialogBox ref="modalEditSpringBootAppDlg"
                                   title="Edit SpringBoot App"
                                   contentReferenceName="springBootAppEditForm"
                                   okCallback={this.okEditCallback}/>

                   <ModalDialogBox ref="confirmDeleteSpringBootAppDlg"
                                   okLabel="Yes"
                                   okCallback={this.confirmDeleteCallback}
                                   cancelLabel="No"/>
               </div>;
    },
    componentDidMount: function() {
        this.loadTableData();
    },
    loadTableData: function(afterLoadCallback) {
        var self = this;
        ServiceFactory.getSpringBootAppService().getAllSpringBootApp().then((function(response){
                                             self.refs.dataTable.refresh(response.applicationResponseContent);
//                                             if ($.isFunction(afterLoadCallback)) {
//                                                 afterLoadCallback();
//                                             }
                                         })).caught(function(response){
                                             console.log(response);
                                             // $.errorAlert(response);
                                         });
    },
    addBtnCallback: function() {
        this.refs.modalAddSpringBootAppDlg.show();
    },
    okAddCallback: function() {
        var self = this;
        if (this.refs.springBootAppAddForm.isValid()) {
            this.refs.springBootAppAddForm.setBusy(true);
            this.setState({showScreenOverlay: true});
            ServiceFactory.getSpringBootAppService().createSpringBootApp(new FormData(this.refs.springBootAppAddForm.refs.form.getDOMNode()))
            .then(function(response){
                self.refs.modalAddSpringBootAppDlg.close();
                self.loadTableData();
            }).caught(function(response){
                $.errorAlert(JSON.parse(response.responseText).message);
            }).lastly(function(){
                if (self.refs.springBootAppAddForm) {
                    self.refs.springBootAppAddForm.setBusy(false);
                }
                self.setState({showScreenOverlay: false});
             });
        }
    },
    okEditCallback: function() {
        var self = this;
        if (this.refs.modalEditSpringBootAppDlg.refs.springBootAppEditForm.isValid()) {
            ServiceFactory.getSpringBootAppService().updateSpringBootApp($(this.refs.modalEditSpringBootAppDlg.refs.springBootAppEditForm.refs.form.getDOMNode()).serializeArray())
            .then(function(response){
                self.refs.modalEditSpringBootAppDlg.close();
                self.loadTableData(function(){
                    self.state.selectedSpringBootApp = self.refs.dataTable.getSelectedItem();
                });
            }).caught(function(response){
                $.errorAlert(JSON.parse(response.responseText).message);
            });
        }
    },
    selectItemCallback: function(item) {
        this.state.selectedSpringBootApp = item;
    },
    deselectAllRowsCallback: function() {
        this.state.selectedSpringBootApp = null;
    },
    delBtnCallback: function() {
        if (this.state.selectedSpringBootApp) {
            this.refs.confirmDeleteSpringBootAppDlg.show("Delete SpringBoot App", "Are you sure you want to delete " +
                                                 this.state.selectedSpringBootApp["str-name"] + " ?");
        }
    },
    confirmDeleteCallback: function() {
        var self = this;
        self.refs.confirmDeleteSpringBootAppDlg.close();
        ServiceFactory.getSpringBootAppService().deleteSpringBootApp(this.state.selectedSpringBootApp["str-name"])
        .then(function(response){
            self.loadTableData(function(){
                self.state.selectedSpringBootApp = null;
            });
        }).caught(function(response){
            $.errorAlert(JSON.parse(response.responseText).message);
        });
    },
    springBootAppNameRenderCallback: function(name, springBootApp) {
        var self = this;
        return <button className="button-link" onClick={function(){self.onClickSpringBootAppNameLink(springBootApp.id)}}>{name}</button>
    },
    onClickSpringBootAppNameLink: function(id) {
        var self = this;
        springBootAppService.getSpringBootAppById(id).then((function(response){
                                                var formData = {};
                                                formData["id"] = response.applicationResponseContent.id;
                                                formData["name"] = response.applicationResponseContent.name;
                                                formData["hostNames"] = response.applicationResponseContent.hostNames;
                                                formData["jdkMedia"] = response.applicationResponseContent.jdkMedia;
                                                formData["gitHubLink"] = response.applicationResponseContent.gitHubLink;
                                                self.refs.modalEditSpringBootAppDlg.show("Edit SpringBoot App", <SpringBootAppConfigForm formData={formData}/>);
                                           })).caught(
                                                function(response){$.errorAlert(response)
                                           });
    }
})

var SpringBootAppConfigForm = React.createClass({
    mixins: [
        React.addons.LinkedStateMixin
    ],
    getInitialState: function() {
        var name = this.props.formData && this.props.formData.name ? this.props.formData.name : null;
        var hostNames = this.props.formData && this.props.formData.hostNames ? this.props.formData.hostNames : null;
        var jdkMedia = this.props.formData && this.props.formData.jdkMedia ? this.props.formData.jdkMedia : null;
        var gitHubLink = this.props.formData && this.props.formData.gitHubLink ? this.props.formData.gitHubLink : null;

        return {name: name, hostNames: hostNames, archiveFilename: "", archiveFile: null, jdkMedia: jdkMedia, gitHubLink: gitHubLink, showUploadBusy: false, jdkVersions: []};
    },
    render: function() {
        var idTextHidden = null;
        var archiveFileInput = null;
        var uploadBusyImg = this.state.showUploadBusy ? <span>Uploading {this.state.archiveFilename} ...
                    <img className="uploadMediaBusyIcon" src="public-resources/img/busy-circular.gif"/></span>: null;

        if (this.props.formData && this.props.formData.id) {
            idTextHidden = <input type="hidden" name="id" value={this.props.formData.id}/>;
        } else {
            archiveFileInput = <div>
                                    <label>Jar or War File</label>
                                        <label htmlFor="archiveFile" className="error"/>
                                        <input type="file" ref="archiveFile" name="archiveFile" required accept=".jar,*.war"
                                               value={this.state.mediaArchiveFilename} onChange={this.onChangeMediaArchiveFile}/>
                                </div>;
        }

        return <div>
                   <form ref="form" enctype="multipart/form-data">
                       {idTextHidden}
                       <label>Name</label>
                       <label htmlFor="name" className="error"/>
                       <input ref="springBootAppName" name="name" type="text" valueLink={this.linkState("name")} maxLength="255" required autoFocus/>

                       <label>Host Name(s)</label>
                       <label htmlFor="hostNames" className="error"/>
                       <input name="hostNames" type="text" valueLink={this.linkState("hostNames")} maxLength="255" required autoFocus/>

                       <label>GitHub Link</label>
                       <label htmlFor="gitHubLink" className="error"/>
                       <input name="gitHubLink" type="text" valueLink={this.linkState("gitHubLink")} maxLength="255" required autoFocus/>

                       {archiveFileInput}

                       <label>JDK</label>
                       <label htmlFor="jdkMedia" className="error"/>
                       <select name="jdkMedia" ref="jdkVersion" valueLink={this.linkState("jdkVersion")}>
                           {this.getJdkVersions()}
                       </select>

                   </form>
                   <div>
                    {uploadBusyImg}
                   </div>
               </div>
    },
    getJdkVersions: function() {
        var items = [<option key='no-jdk-version' value=''>--- Select JDK ---</option>];
        for (var i=0; i < this.state.jdkVersions.length; i++){
            var jdkVersionOption = this.state.jdkVersions[i];
            var selected = this.state.jdkMedia !== null && jdkVersionOption.id === this.state.jdkMedia.id;
            items.push(<option key={"jdk-version-" + jdkVersionOption.id} selected={selected}
                               value={jdkVersionOption.id}>{jdkVersionOption.name}</option>);
        }
        return items;
    },
    validator: null,
    componentDidMount: function() {
        var self = this;
        if (self.validator === null) {
            self.validator = $(self.refs.form.getDOMNode()).validate({rules:{"jdkMediaId":{"required":true}, "archiveFile":{"required":false}, "gitHubLink":{"required":false}}});
        }
        this.getMedia();
        $(this.refs.springBootAppName.getDOMNode()).focus();
    },
    getMedia: function() {
        var self = this;
        mediaService.getAllMedia().then((function(response){
            var allMedia = response.applicationResponseContent;
            var jdkVersions = [];
            for (var i = 0; i < allMedia.length; i++) {
                if (allMedia[i].type.name === "JDK") {
                    jdkVersions.push(allMedia[i]);
                }
            }
            self.setState({jdkVersions: jdkVersions});
        })).caught(function(response){
            $.errorAlert(response);
        });
    },
    isValid: function() {
        if (this.validator !== null) {
            this.validator.form();
            if (this.validator.numberOfInvalids() === 0) {
                return true;
            }
        } else {
            alert("There is no validator for the form!");
        }
        return false;
    },
    componentWillUnmount: function() {
        this.state.showUploadBusy = false;
    },
    onChangeMediaArchiveFile: function(e) {
        this.setState({mediaArchiveFilename: this.refs.mediaArchiveFile.getDOMNode().value,
                       mediaArchiveFile: this.refs.mediaArchiveFile.getDOMNode().files[0]});
    },
    getMediaArchiveFile: function() {
        return this.state.mediaArchiveFile;
    },
    setBusy: function(val) {
        this.setState({showUploadBusy: val});
    }
});