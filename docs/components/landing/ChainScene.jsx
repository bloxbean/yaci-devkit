import { useEffect, useRef } from "react";

/**
 * A 3D chain of blocks drifting behind the hero: blocks flow along a gentle wave, new ones light up at the head,
 * and transactions travel along the links. three.js is loaded only on wide screens with WebGL and motion allowed,
 * so phones and reduced-motion visitors never download it.
 */

const COUNT = 30;
const SPACING = 1.45;
const SPEED = 0.55; // world units per second
const PARTICLES = 70;

const pathY = (x) => Math.sin(x * 0.3) * 0.8;
const pathZ = (x) => Math.cos(x * 0.27) * 1.8 - 1.5;

function webglAvailable() {
  try {
    const c = document.createElement("canvas");
    return !!(c.getContext("webgl2") || c.getContext("webgl"));
  } catch {
    return false;
  }
}

export default function ChainScene() {
  const host = useRef(null);

  useEffect(() => {
    const el = host.current;
    if (!el) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    if (window.innerWidth < 900 || !webglAvailable()) return;

    let disposed = false;
    let cleanup = () => {};

    import("three").then((THREE) => {
      if (disposed) return;

      const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, powerPreference: "low-power" });
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
      renderer.setClearColor(0x000000, 0);
      el.appendChild(renderer.domElement);

      const scene = new THREE.Scene();
      scene.fog = new THREE.Fog(0x07080d, 7, 18);
      const camera = new THREE.PerspectiveCamera(38, 1, 0.1, 100);
      camera.position.set(0, 2.2, 8);

      scene.add(new THREE.AmbientLight(0xffffff, 0.35));
      const warm = new THREE.PointLight(0xff5a36, 30, 30, 1.6);
      warm.position.set(6, 3, 6);
      scene.add(warm);
      const cool = new THREE.PointLight(0xa78bfa, 30, 30, 1.6);
      cool.position.set(-8, -2, 5);
      scene.add(cool);

      const geo = new THREE.BoxGeometry(0.62, 0.62, 0.62);
      const edgesGeo = new THREE.EdgesGeometry(geo);
      const span = COUNT * SPACING;
      const minX = -span / 2;

      const blocks = Array.from({ length: COUNT }, (_, i) => {
        const mat = new THREE.MeshStandardMaterial({
          color: 0x0d0a10,
          emissive: 0xff4e2d,
          emissiveIntensity: 0.05,
          metalness: 0.6,
          roughness: 0.25,
          transparent: true,
          opacity: 0.7,
        });
        const mesh = new THREE.Mesh(geo, mat);
        const edges = new THREE.LineSegments(edgesGeo, new THREE.LineBasicMaterial({ color: 0xff7a45, transparent: true, opacity: 0.85 }));
        mesh.add(edges);
        mesh.userData = { x: minX + i * SPACING, spin: (Math.random() - 0.5) * 0.4, born: -10 };
        mesh.rotation.set(Math.random() * 0.6, Math.random() * Math.PI, 0);
        scene.add(mesh);
        return mesh;
      });

      // Links between consecutive blocks
      const linkGeo = new THREE.BufferGeometry();
      const linkPos = new Float32Array(COUNT * 3);
      linkGeo.setAttribute("position", new THREE.BufferAttribute(linkPos, 3));
      const link = new THREE.Line(linkGeo, new THREE.LineBasicMaterial({ color: 0xff8a3d, transparent: true, opacity: 0.28 }));
      scene.add(link);

      // Transactions travelling along the chain
      const pGeo = new THREE.BufferGeometry();
      const pPos = new Float32Array(PARTICLES * 3);
      const pOffset = Array.from({ length: PARTICLES }, () => ({ t: Math.random(), v: 0.04 + Math.random() * 0.05, r: Math.random() * 0.5 }));
      pGeo.setAttribute("position", new THREE.BufferAttribute(pPos, 3));
      const particles = new THREE.Points(
        pGeo,
        new THREE.PointsMaterial({ color: 0x22d3ee, size: 0.11, transparent: true, opacity: 0.9, depthWrite: false })
      );
      scene.add(particles);

      const pointer = { x: 0, y: 0 };
      const onPointer = (e) => {
        pointer.x = e.clientX / window.innerWidth - 0.5;
        pointer.y = e.clientY / window.innerHeight - 0.5;
      };
      window.addEventListener("pointermove", onPointer, { passive: true });

      const resize = () => {
        const w = el.clientWidth;
        const h = el.clientHeight;
        if (!w || !h) return;
        renderer.setSize(w, h, false);
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
      };
      const ro = new ResizeObserver(resize);
      ro.observe(el);
      resize();

      let visible = true;
      const io = new IntersectionObserver(([entry]) => {
        visible = entry.isIntersecting;
      });
      io.observe(el);

      const clock = new THREE.Clock();
      let raf = 0;
      const tick = () => {
        raf = requestAnimationFrame(tick);
        if (!visible) {
          clock.getDelta();
          return;
        }
        const dt = Math.min(clock.getDelta(), 0.05);
        const now = clock.elapsedTime;
        const light = document.documentElement.getAttribute("data-dk-theme") === "light";
        scene.fog.color.set(light ? 0xfaf8f6 : 0x07080d);

        let headX = -Infinity;
        blocks.forEach((b) => {
          b.userData.x -= SPEED * dt;
          if (b.userData.x < minX) {
            b.userData.x += span;
            b.userData.born = now; // a new block appears at the head
          }
          headX = Math.max(headX, b.userData.x);
        });

        blocks.forEach((b) => {
          const { x, spin, born } = b.userData;
          b.position.set(x, pathY(x), pathZ(x));
          b.rotation.y += spin * dt;
          b.rotation.x += spin * 0.4 * dt;
          const age = now - born;
          const grow = Math.min(1, age / 0.6);
          b.scale.setScalar(0.35 + 0.65 * (1 - Math.pow(1 - grow, 3)));
          const isHead = x === headX;
          const flash = Math.max(0, 1 - age / 1.6);
          b.material.emissiveIntensity = 0.05 + (isHead ? 1.1 : 0) + flash * 1.6;
          b.material.opacity = isHead || flash > 0 ? 0.95 : 0.7;
          b.material.color.set(light ? 0xffb493 : 0x0d0a10);
          b.children[0].material.opacity = light ? 0.9 : 0.85;
        });
        // Keep the link line ordered by x so it never jumps across the scene
        const order = blocks.map((b) => b.userData.x).sort((a, c) => a - c);
        order.forEach((x, i) => {
          linkPos[i * 3] = x;
          linkPos[i * 3 + 1] = pathY(x);
          linkPos[i * 3 + 2] = pathZ(x);
        });
        linkGeo.attributes.position.needsUpdate = true;

        pOffset.forEach((p, i) => {
          p.t = (p.t + p.v * dt) % 1;
          const x = minX + p.t * span;
          const a = now * 2 + i;
          pPos[i * 3] = x;
          pPos[i * 3 + 1] = pathY(x) + Math.sin(a) * p.r * 0.5;
          pPos[i * 3 + 2] = pathZ(x) + Math.cos(a) * p.r * 0.5;
        });
        pGeo.attributes.position.needsUpdate = true;

        camera.position.x += (pointer.x * 1.6 - camera.position.x) * 0.04;
        camera.position.y += (2.2 - pointer.y * 0.8 - camera.position.y) * 0.04;
        camera.lookAt(0, 0, -1.5);
        renderer.render(scene, camera);
      };
      tick();
      requestAnimationFrame(() => el.classList.add("on"));

      cleanup = () => {
        cancelAnimationFrame(raf);
        window.removeEventListener("pointermove", onPointer);
        ro.disconnect();
        io.disconnect();
        blocks.forEach((b) => {
          b.material.dispose();
          b.children[0].material.dispose();
        });
        geo.dispose();
        edgesGeo.dispose();
        linkGeo.dispose();
        link.material.dispose();
        pGeo.dispose();
        particles.material.dispose();
        renderer.dispose();
        renderer.domElement.remove();
      };
    });

    return () => {
      disposed = true;
      cleanup();
    };
  }, []);

  return <div ref={host} className="dk-chain3d" aria-hidden="true" />;
}
