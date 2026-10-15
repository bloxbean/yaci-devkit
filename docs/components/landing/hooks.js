import { useCallback, useEffect, useRef, useState } from "react";

/** True while the element is on screen; used to pause timers for off-screen illustrations. */
export function useInView(rootMargin = "80px") {
  const ref = useRef(null);
  const [inView, setInView] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el || typeof IntersectionObserver === "undefined") {
      setInView(true);
      return;
    }
    const io = new IntersectionObserver(([entry]) => setInView(entry.isIntersecting), { rootMargin });
    io.observe(el);
    return () => io.disconnect();
  }, [rootMargin]);
  return [ref, inView];
}

/** Runs `fn` every `ms` while `active` is true. */
export function useInterval(fn, ms, active = true) {
  const saved = useRef(fn);
  saved.current = fn;
  useEffect(() => {
    if (!active) return;
    const id = setInterval(() => saved.current(), ms);
    return () => clearInterval(id);
  }, [ms, active]);
}

export function usePrefersReducedMotion() {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
    setReduced(mq.matches);
    const onChange = () => setReduced(mq.matches);
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, []);
  return reduced;
}

/** Landing theme, shared with the docs pages through next-themes' "theme" key. */
export function useTheme() {
  const [theme, setTheme] = useState("dark");
  useEffect(() => {
    let saved = null;
    try {
      saved = localStorage.getItem("theme");
    } catch {}
    const current =
      saved === "light" || saved === "dark"
        ? saved
        : window.matchMedia("(prefers-color-scheme: light)").matches
          ? "light"
          : "dark";
    document.documentElement.setAttribute("data-dk-theme", current);
    setTheme(current);
  }, []);
  const toggle = useCallback(() => {
    setTheme((prev) => {
      const next = prev === "dark" ? "light" : "dark";
      document.documentElement.setAttribute("data-dk-theme", next);
      try {
        localStorage.setItem("theme", next);
      } catch {}
      return next;
    });
  }, []);
  return [theme, toggle];
}

export function useGithubStars(repo) {
  const [stars, setStars] = useState(null);
  useEffect(() => {
    let cancelled = false;
    const key = `dk-stars-${repo}`;
    try {
      const cached = sessionStorage.getItem(key);
      if (cached) {
        setStars(Number(cached));
        return;
      }
    } catch {}
    fetch(`https://api.github.com/repos/${repo}`)
      .then((r) => (r.ok ? r.json() : null))
      .then((d) => {
        if (cancelled || !d || typeof d.stargazers_count !== "number") return;
        setStars(d.stargazers_count);
        try {
          sessionStorage.setItem(key, String(d.stargazers_count));
        } catch {}
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [repo]);
  return stars;
}

/**
 * Fades sections in as they scroll into view. Content stays visible until this runs, so a page whose scripts are
 * slow to load is never blank: what's already on screen (and the hero, which animates in with CSS) stays shown, and
 * only what's further down is hidden until it scrolls in.
 */
export function useReveal() {
  useEffect(() => {
    if (typeof IntersectionObserver === "undefined") return;
    const root = document.documentElement;
    const later = [];
    document.querySelectorAll(".dk-reveal").forEach((el) => {
      if (el.closest(".dk-hero") || el.getBoundingClientRect().top < window.innerHeight) el.classList.add("in");
      else later.push(el);
    });
    root.setAttribute("data-dk-js", "");
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            e.target.classList.add("in");
            io.unobserve(e.target);
          }
        });
      },
      { rootMargin: "0px 0px -8% 0px", threshold: 0.08 }
    );
    later.forEach((el) => io.observe(el));
    return () => {
      io.disconnect();
      root.removeAttribute("data-dk-js");
    };
  }, []);
}

export function useScrolled(offset = 12) {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > offset);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, [offset]);
  return scrolled;
}

export const formatNumber = (n) => n.toLocaleString("en-US");

/** A short hex string, stable for a given seed so server and client render the same value. */
export function hex(seed, len = 4) {
  let x = (seed * 2654435761) >>> 0;
  let out = "";
  while (out.length < len) {
    x = (x * 1103515245 + 12345) >>> 0;
    out += (x >>> 16).toString(16).padStart(4, "0");
  }
  return out.slice(0, len);
}
