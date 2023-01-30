const path = require('path');

module.exports = {
  mode: 'development',
  entry: {
    index: ['./src/bbooru.js'],
    global: ['./src/globalTemplates.js'],
    results: ['./src/results/resultsPage.js'],
    viewer: ['./src/viewer/viewer.js']
  },
  output: {
    filename: '[name].js',
    path: path.resolve(__dirname, 'dist'),
  },
};