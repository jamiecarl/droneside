const webpack = require("@nativescript/webpack");

module.exports = (env) => {
	webpack.init(env);

	// Learn how to customize:
	// https://docs.nativescript.org/webpack

	const config = webpack.resolveConfig();

	// Add fallbacks for Node.js core modules that are no longer polyfilled in webpack 5
	config.resolve = config.resolve || {};
	config.resolve.fallback = {
		...config.resolve.fallback,
		"url": false,
		"util": false,
		"path": false,
		"fs": false,
		"os": false
	};

	return config;
};
