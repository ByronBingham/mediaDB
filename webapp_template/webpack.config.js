const path = require('path');
const CopyPlugin = require("copy-webpack-plugin");

module.exports = {
  //mode: 'development',
  entry: {
    index: ['./src/webapp_main.js'],
    global: ['./src/globalTemplates.js'],
    results: ['./src/results/resultsPage.js'],
    viewer: ['./src/viewer/viewer.js']
  },
  output: {
    filename: '[name].js',
    path: path.resolve(__dirname, 'dist'),
  },
  plugins: [
    new CopyPlugin({
      patterns: [
        { from: "template_files", to: "." },
        { from: "WEB-INF", to: "WEB-INF" }
      ],
    })
  ]
};