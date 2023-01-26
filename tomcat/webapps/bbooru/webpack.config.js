const path = require('path');

module.exports = {
  entry: {
    index: ['./src/bbooru.js'],
    results: ['./src/results/resultsPage.js']
  },
  output: {
    filename: '[name].js',
    path: path.resolve(__dirname, 'dist'),
  },
};