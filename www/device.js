//255 ->0f0f , 1->0001 ,128->0800 等。4ビットずつ、あわせて8ビットおくるよ
function int2bytesStr(num){
    var hexStr = num.toString(16);
    hex1 = hexStr.slice(-2,-1);
    hex2 = hexStr.slice(-1);
    if(!hex1){
        hex1= "0";
    }
    return "0"+hex1 + "0" + hex2;
}

//[0,10,255] -> "0000000a0f0f"
function intArray2bytesStr(arr){
    str = "";
    for(var i=0;i<arr.length;i++){
        str= str +int2bytesStr(arr[i]);
    }
    return str;
}
var  exec = require('cordova/exec'),
cordova = require('cordova');


function Device() {
}


Device.prototype.getInfo = function(successCallback, errorCallback) {
    exec(successCallback, errorCallback, "Device", "テストアクション", ["testargs@device.js"]);
};
Device.prototype.connect = function(successCallback, errorCallback) {
    exec(successCallback, errorCallback, "Device", "connect", ["testargs@device.js"]);
};

/**
* @list 0~255の整数の配列
*/
Device.prototype.miniSend = function(list,successCallback, errorCallback){
    str= intArray2bytesStr(list);
    exec(successCallback,errorCallback,"Device","miniSend",[str]);
};

module.exports = new Device();


