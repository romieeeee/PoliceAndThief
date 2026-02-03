module.exports = {
    apps: [
        {
            name: "gps-consumer-server",
            script: "./src/app.js",
            exec_mode: "cluster",
            watch: false,
            instances: 4
        }
    ]
}
