const releaseLabel = document.querySelector("#release-label");
const copyright = document.querySelector("#copyright");

copyright.textContent = `© ${new Date().getFullYear()} Biblia Diaria`;

fetch("https://github.com/jahruz67/Bible-Daily/releases/download/latest/latest.json", {
  cache: "no-store",
})
  .then((response) => {
    if (!response.ok) throw new Error("Release metadata unavailable");
    return response.json();
  })
  .then((release) => {
    if (release.versionName) releaseLabel.textContent = `Versión ${release.versionName} disponible`;
  })
  .catch(() => {
    releaseLabel.textContent = "Última versión disponible";
  });

const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add("visible");
        observer.unobserve(entry.target);
      }
    });
  },
  { threshold: 0.12 },
);

document.querySelectorAll(".reveal").forEach((element) => observer.observe(element));
