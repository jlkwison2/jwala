/** @jsx React.DOM */
var SpringBootDashboard = React.createClass({
    getInitialState: function() {
        return {springBootApps: null, selectedApp: null}
    },
    render: function() {
        if (this.state.springBootApps) {
            return <div className="SpringBootDashboard">
                       <LeftPanel springBootApps={this.state.springBootApps} selectCallback={this.selectCallback}/>
                       <RightPanel springBootApps={this.state.springBootApps} selectedApp={this.state.selectedApp}/>
                   </div>
        }
        return <div>Loading data...</div>
    },
    componentDidMount: function() {
        let self = this;
        springBootAppService.getAllSpringBootApp().then(function(response){
            if (response.applicationResponseContent && response.applicationResponseContent.length > 0) {
                self.setState({springBootApps: response.applicationResponseContent, selectedApp: response.applicationResponseContent[0]});
            } else {
                self.setState({springBootApps: []});
            }
        }).caught(function(response){
            console.log(response);
        });
    },
    selectCallback: function(selectedItem) {
        let self = this;
        this.state.springBootApps.forEach(function(app){
            if (app.name === selectedItem) {
                self.setState({selectedApp: app});
                return;
            }
        });
    }
})

var LeftPanel = React.createClass({
    render: function() {
        let appNames = [];
        this.props.springBootApps.forEach(function(app){
            appNames.push(app.name);
        });
        return <div className="LeftPanel">
                   <SelectionMenu ref="selectionMenu" heading="APPLICATIONS" menuItems={appNames} selectCallback={this.selectCallback}/>
               </div>
    },
    selectCallback: function(selectedItem) {
        this.props.selectCallback(selectedItem);
    }
});

var RightPanel = React.createClass({
    render: function() {
        return <div className="RightPanel">
                   <ServerTable selectedApp={this.props.selectedApp}/>
               </div>
    }
});

var ServerTable = React.createClass({
    timeoutHandler: null,
    servers: null,
    render: function() {
        this.servers = this.props.selectedApp.hostNames.split(",");
        let appName = this.props.selectedApp.name;
        let trArray = [];
        let self = this;
        this.servers.forEach(function(server){
            trArray.push(<tr>
                           <td><ToggleButton ref={server + "_toggle"} on={null} server={server} onClick={self.onToggleButtonClick.bind(self, appName, server)}/></td>
                           <td>{server}</td>
                           <td>8080</td>
                           <td>10.142.0.2</td>
                           <td><i id={server + "_status"} className="fa fa-circle" style={{color:"white"}} aria-hidden="true"></i></td>
                           <td><button onClick={self.onDeployButtonClick.bind(self, appName, server)}>Deploy</button></td>
                         </tr>);
        });

        return <table>
                  <thead>
                      <th></th><th>Name</th><th>Port</th><th>Internal IP</th><th>Status</th><th></th>
                  </thead>
                  <tbody>
                    {trArray}
                  </tbody>
              </table>
    },
    onToggleButtonClick: function(name, server, on) {
        if (on) {
            springBootAppService.startSpringBootApp(name, server).then(function(response){

            }).caught(function(err){

            });
            return;
        }

        springBootAppService.stopSpringBootApp(name, server).then(function(response){
        }).caught(function(err){
        });

    },
    onDeployButtonClick: function(name, server) {
        springBootAppService.generateAndDeploySpringBootApp(name)
        .then(function(response){
            $.alert("Successfully deployed " + name);
        }).caught(function(err){
            $.errorAlert(err.responseJSON.message);
        });
    },
    componentDidMount: function() {
        this.timeoutHandler = setTimeout(this.timeoutCallback, 500);
        console.log("Timeout " + this.timeoutHandler + " created");
    },
    timeoutCallback: function() {
        clearTimeout(this.timeoutHandler);
        let self = this;
        self.servers.forEach(function(server){
            let url = "http://" + server + ":8080/"
            springBootAppService.getUrlResponse(url).then(function(response){
                $("#" + server + "_status").css("color", "green");
                self.refs[server + "_toggle"].setOn(true);
            }).caught(function(err){
                console.log(err);
                $("#" + server + "_status").css("color", "red");
                self.refs[server + "_toggle"].setOn(false);
            }).lastly(function(){
                self.timeoutHandler = setTimeout(self.timeoutCallback, 5000);
            });
        });
    },
    componentWillUnmount: function() {
        console.log("Clearing timeout " + this.timeoutHandler);
        clearTimeout(this.timeoutHandler);
    }

});

var ToggleButton = React.createClass({
    getInitialState: function() {
        // return {on: !this.props.on ? false : true};
        return {on: this.props.on, disabled: false};
    },
    render: function() {
        if (this.state.on === null) {
            return <span>. . .</span>;
        }

        if (this.state.on) {
            return <i className="fa fa-toggle-on" aria-hidden="true" onClick={this.onClick}/>
        }
        return <i className="fa fa-toggle-off" aria-hidden="true" onClick={this.onClick}/>
    },
    onClick: function() {
        let on = !this.state.on;
        this.setState({on: on});
        if (this.props.onClick) {
            this.props.onClick(on);
        }
    },

    setOn: function(on) {
        if (this.state.on === null) {
            this.setState({on: on});
        }
    },

    disable: function(val) {
        this.state.disabled(val);
    }
})

var SelectionMenu = React.createClass({
    getInitialState: function() {
        return {selectedItem: this.props.menuItems[0]};
    },
    render: function() {
        let menuItems = [];
        let self = this;
        this.props.menuItems.forEach(function(menuItem) {
            if (self.state.selectedItem && self.state.selectedItem === menuItem) {
                menuItems.push(<li><i className="fa fa-circle" aria-hidden="true"></i><span onClick={function(){self.onMenuItemClick(menuItem)}}>{menuItem}</span></li>);
            } else {
                menuItems.push(<li className="unselected"><span onClick={function(){self.onMenuItemClick(menuItem)}}>{menuItem}</span></li>);
            }

        });
        return <div className="SelectionMenu">
                   <h4>{this.props.heading}</h4>
                   <ul className="springBootAppList">
                       {menuItems}
                   </ul>
               </div>
    },
    onMenuItemClick: function(menuItem) {
        this.setState({selectedItem: menuItem});
        this.props.selectCallback(menuItem);
    }
})

