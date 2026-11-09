(() => {
    function initEntitySearch() {
        const modal = document.getElementById("entitySearchModal");
        const trigger = document.getElementById("openEntitySearch");
        const input = document.getElementById("entitySearchInput");
        if (!modal || !trigger || !input) return;

        const results = document.getElementById("entitySearchResults");
        const emptyState = document.getElementById("entitySearchEmpty");
        const sortOrder = document.getElementById("entitySortOrder");
        let lastFocusedElement = null;

        function updateResults() {
            const query = input.value.trim().toLocaleLowerCase();
            const entities = Array.from(results.querySelectorAll(".entity-search-result"));

            entities.forEach((entity) => {
                entity.hidden = !entity.dataset.search.toLocaleLowerCase().includes(query);
            });

            entities.sort((first, second) => {
                const comparison = first.dataset.name.localeCompare(second.dataset.name, "es", {
                    sensitivity: "base"
                });
                return sortOrder.value === "desc" ? -comparison : comparison;
            }).forEach((entity) => results.appendChild(entity));

            emptyState.hidden = entities.some((entity) => !entity.hidden);
        }

        function openModal() {
            lastFocusedElement = document.activeElement;
            modal.classList.add("is-open");
            modal.setAttribute("aria-hidden", "false");
            document.body.classList.add("entity-search-open");
            input.focus();
        }

        function closeModal() {
            modal.classList.remove("is-open");
            modal.setAttribute("aria-hidden", "true");
            document.body.classList.remove("entity-search-open");
            if (lastFocusedElement && typeof lastFocusedElement.focus === "function") {
                lastFocusedElement.focus();
            }
        }

        trigger.addEventListener("click", openModal);
        modal.querySelectorAll("[data-close-entity-search]").forEach((button) => {
            button.addEventListener("click", closeModal);
        });
        input.addEventListener("input", updateResults);
        sortOrder.addEventListener("change", updateResults);

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && modal.classList.contains("is-open")) {
                closeModal();
            }
        });

        updateResults();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initEntitySearch);
    } else {
        initEntitySearch();
    }
})();
