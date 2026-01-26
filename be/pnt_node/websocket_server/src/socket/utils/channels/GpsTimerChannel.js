const gpsTimerChannel = async (message, pubClient, gameIo) => {

    setInterval(() => {
        gameIo.sockets.adapter.rooms
    }, 1000);
    const locations = await pubClient.hgetall(message);
    
}

export default gpsTimerChannel;