import express from "express";
import cors from "cors";

const port = 8090;
const app = express();

app.use(cors());
app.use(express.json());

app.get("/", (req, res) => {
    console.log("request in root");
    res.status(200).json({"message" : "ok"});
})


app.listen(port, () => {
    console.log("Listen in port:", port);
});