const express = require('express');
const app = express();

app.use(express.static(__dirname)); // Serve all files in current directory

app.get('/', (req, res) => {
    res.sendFile(__dirname + '/index.html');
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
    console.log(`Static Web App running on port ${PORT}`);
});

exports.app = app;
