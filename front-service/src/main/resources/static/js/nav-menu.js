(() => {
    const toggle = document.getElementById("navMenuToggle");
    const menu = document.getElementById("navMenu");
    const header = document.querySelector(".site-nav");
    if (!toggle || !menu || !header) return;

    function closeMenu(returnFocus = false) {
        menu.classList.remove("is-open");
        toggle.setAttribute("aria-expanded", "false");
        toggle.setAttribute("aria-label", "Abrir menú");
        if (returnFocus) toggle.focus();
    }

    toggle.addEventListener("click", () => {
        const isOpen = menu.classList.toggle("is-open");
        toggle.setAttribute("aria-expanded", String(isOpen));
        toggle.setAttribute("aria-label", isOpen ? "Cerrar menú" : "Abrir menú");
    });

    menu.querySelectorAll("a").forEach((link) => {
        link.addEventListener("click", () => closeMenu());
    });

    document.addEventListener("click", (event) => {
        if (!header.contains(event.target)) closeMenu();
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && menu.classList.contains("is-open")) {
            closeMenu(true);
        }
    });

    window.addEventListener("resize", () => {
        if (window.innerWidth > 1024) closeMenu();
    });
})();
