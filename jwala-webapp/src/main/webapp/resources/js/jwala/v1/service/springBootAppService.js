var springBootAppService = {
    getAllSpringBootApp: function() {
        return serviceFoundation.promisedGet("v1.0/springboot", "json");
    },
    getSpringBootAppById: function(id) {
        return serviceFoundation.promisedGet("v1.0/springboot/app/" + id, "json");
    },
    getSpringBootAppByName: function(name, responseCallback) {
        return serviceFoundation.promisedGet("v1.0/springboot;name=" + encodeURIComponent(name), "json");
    },
    createSpringBootApp: function(formData) {
        return serviceFoundation.promisedPost("v1.0/springboot", "json", formData, null, true, false);
    },
    updateSpringBootApp: function(serializedArray) {
        var jsonData = {};
        serializedArray.forEach(function(item){
            jsonData[item.name] = item.value;
        });
        return serviceFoundation.promisedPut("v1.0/springboot", "json", JSON.stringify(jsonData));
    },
    deleteSpringBootApp: function(name) {
        return serviceFoundation.promisedDel("v1.0/springboot/" + encodeURIComponent(name), "json");
    },
    getUrlResponse: function(url) {
        return serviceFoundation.promisedGet("v1.0/springboot/url?val=" + encodeURIComponent(url), "json");
    },
    startSpringBootApp: function(name, host) {
        return serviceFoundation.promisedPut("v1.0/springboot/control/" + encodeURIComponent(name) + "/START/" + host);
    },
    stopSpringBootApp: function(name, host) {
        return serviceFoundation.promisedPut("v1.0/springboot/control/" + encodeURIComponent(name) + "/STOP/" + host);
    },
    generateAndDeploySpringBootApp: function(name) {
        return serviceFoundation.promisedPut("v1.0/springboot/generate/" + encodeURIComponent(name));
    }
};
