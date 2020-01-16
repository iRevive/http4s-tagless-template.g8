/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

// See https://docusaurus.io/docs/site-config for all the possible
// site configuration options.

const repoUrl = "https://your-repo.com";

const siteConfig = {
    title: "$name_normalized$",
    tagline: "The $name_normalized$ documentation",
    url: 'https://your-docusaurus-test-site.com', // Your website URL
    baseUrl: '/$name_normalized$/',
    projectName: "$name_normalized$",

    // For no header links in the top nav bar -> headerLinks: [],
    headerLinks: [
        { href: repoUrl, label: "Repository", external: true }
    ],

    customDocsPath: "$name_normalized$-docs/target/mdoc",

    /* Colors for website */
    colors: {
        primaryColor: '#2e8555',
        secondaryColor: '#20232a',
    },

    // This copyright info is used in /core/Footer.js and blog RSS/Atom feeds.
    copyright: `Copyright © \${new Date().getFullYear()} $organization$`,

    highlight: {
        // Highlight.js theme to use for syntax highlighting in code blocks.
        theme: 'github',
    },

    // On page navigation for the current documentation page.
    onPageNav: 'separate',

    // For sites with a sizable amount of content, set collapsible to true.
    // Expand/collapse the links and subcategories under categories.
    // docsSideNavCollapsible: true,

    // Show documentation's last contributor's name.
    // enableUpdateBy: true,

    // Show documentation's last update time.
    enableUpdateTime: true,

    // You may provide arbitrary config keys to be used as needed by your
    // template. For example, if you need your repo's URL...
    repoUrl: repoUrl,
};

module.exports = siteConfig;
