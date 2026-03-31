/**
 * Manages the light/dark mode for the Hodor Login Page.
 * Stores preference in localStorage and respects system settings.
 * 
 * @author Hodor Agent
 */
document.addEventListener('DOMContentLoaded', () => {
    const themeToggle = document.getElementById('themeToggle');
    const themeIcon = document.getElementById('themeIcon');
    const body = document.body;

    /**
     * Set the theme and update the UI (Icon/Attribute).
     * @param {string} theme 'light' | 'dark'
     */
    const setTheme = (theme) => {
        if (theme === 'dark') {
            document.documentElement.setAttribute('data-theme', 'dark');
            // Update Icon to Sun for Switching to Light
            themeIcon.innerHTML = `<circle cx="12" cy="12" r="5"></circle>
                                  <line x1="12" y1="1" x2="12" y2="3"></line>
                                  <line x1="12" y1="21" x2="12" y2="23"></line>
                                  <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                                  <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                                  <line x1="1" y1="12" x2="3" y2="12"></line>
                                  <line x1="21" y1="12" x2="23" y2="12"></line>
                                  <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                                  <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>`;
            localStorage.setItem('hodor-theme', 'dark');
        } else {
            document.documentElement.setAttribute('data-theme', 'light');
            // Update Icon back to Moon for Switching to Dark
            themeIcon.innerHTML = `<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>`;
            localStorage.setItem('hodor-theme', 'light');
        }
    };

    // 1. Initial State: Check Storage -> Then System Preference
    const savedTheme = localStorage.getItem('hodor-theme');
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

    if (savedTheme) {
        setTheme(savedTheme);
    } else if (prefersDark) {
        setTheme('dark');
    } else {
        setTheme('light');
    }

    // 2. Event Listener for Manual Toggle
    themeToggle.addEventListener('click', () => {
        const currentTheme = document.documentElement.getAttribute('data-theme');
        if (currentTheme === 'dark') {
            setTheme('light');
        } else {
            setTheme('dark');
        }
    });
});
