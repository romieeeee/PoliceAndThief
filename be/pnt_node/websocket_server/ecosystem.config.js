module.exports = {
    apps : [
        {
            name : "websocket-server",
            script: "./src/server-register.js",
            exec_mode: "cluster",
            watch: false,
            instances: 4
        }
    ]
}