import { useEffect, useRef, useState, useCallback } from "react";

/* ─── helpers ─── */
const cn = (...c: (string | false | undefined)[]) => c.filter(Boolean).join(" ");

/* ─── Frequency Visualizer Canvas (Hero) ─── */
function FrequencyCanvas({ className }: { className?: string }) {
  const ref = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const c = ref.current!;
    const ctx = c.getContext("2d")!;
    let raf: number;
    const dpr = window.devicePixelRatio || 1;
    const resize = () => {
      c.width = c.offsetWidth * dpr;
      c.height = c.offsetHeight * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    resize();
    window.addEventListener("resize", resize);

    const barCount = 64;
    const phases = Array.from({ length: barCount }, () => Math.random() * Math.PI * 2);
    const speeds = Array.from({ length: barCount }, () => 0.8 + Math.random() * 2.5);
    const baseHeights = Array.from({ length: barCount }, (_, i) => {
      const center = barCount / 2;
      const dist = Math.abs(i - center) / center;
      return 0.3 + (1 - dist * dist) * 0.7;
    });

    let t = 0;
    const draw = () => {
      const W = c.offsetWidth;
      const H = c.offsetHeight;
      ctx.clearRect(0, 0, W, H);
      t += 0.016;

      const barW = W / barCount;
      const maxH = H * 0.6;

      for (let i = 0; i < barCount; i++) {
        const wave = Math.sin(t * speeds[i] + phases[i]);
        const wave2 = Math.sin(t * 0.7 + i * 0.15);
        const h = maxH * baseHeights[i] * (0.4 + 0.6 * (0.5 + 0.5 * wave)) * (0.7 + 0.3 * wave2);
        const x = i * barW;
        const y = H - h;

        const hue = 260 + (i / barCount) * 60; // violet → magenta
        const sat = 70 + wave * 20;
        const light = 55 + wave * 15;

        const g = ctx.createLinearGradient(x, y, x, H);
        g.addColorStop(0, `hsla(${hue}, ${sat}%, ${light}%, 0.9)`);
        g.addColorStop(0.5, `hsla(${hue}, ${sat}%, ${light - 15}%, 0.4)`);
        g.addColorStop(1, `hsla(${hue}, ${sat}%, ${light - 25}%, 0.05)`);

        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.roundRect(x + 1, y, barW - 2, h, [2, 2, 0, 0]);
        ctx.fill();

        // Top glow dot
        if (h > 20) {
          ctx.fillStyle = `hsla(${hue}, 90%, 80%, ${0.4 + 0.3 * wave})`;
          ctx.beginPath();
          ctx.arc(x + barW / 2, y, 2, 0, Math.PI * 2);
          ctx.fill();
        }
      }

      raf = requestAnimationFrame(draw);
    };
    draw();
    return () => { cancelAnimationFrame(raf); window.removeEventListener("resize", resize); };
  }, []);
  return <canvas ref={ref} className={cn("absolute inset-0 w-full h-full pointer-events-none", className)} />;
}

/* ─── Ambient Particle Drift ─── */
function DriftCanvas({ className }: { className?: string }) {
  const ref = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const c = ref.current!;
    const ctx = c.getContext("2d")!;
    let raf: number;
    const dpr = window.devicePixelRatio || 1;
    const resize = () => {
      c.width = c.offsetWidth * dpr;
      c.height = c.offsetHeight * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    resize();
    window.addEventListener("resize", resize);

    interface Mote { x: number; y: number; r: number; vx: number; vy: number; phase: number; speed: number; hue: number }
    const motes: Mote[] = Array.from({ length: 80 }, () => ({
      x: Math.random(), y: Math.random(),
      r: 0.5 + Math.random() * 2,
      vx: (Math.random() - 0.5) * 0.0003,
      vy: -0.0001 - Math.random() * 0.0004,
      phase: Math.random() * Math.PI * 2,
      speed: 0.5 + Math.random() * 1.5,
      hue: 260 + Math.random() * 80,
    }));

    let t = 0;
    const draw = () => {
      const W = c.offsetWidth, H = c.offsetHeight;
      ctx.clearRect(0, 0, W, H);
      t += 0.016;
      for (const m of motes) {
        m.x += m.vx;
        m.y += m.vy;
        if (m.y < -0.05) { m.y = 1.05; m.x = Math.random(); }
        if (m.x < -0.05 || m.x > 1.05) m.x = Math.random();
        const a = 0.15 + 0.25 * (0.5 + 0.5 * Math.sin(t * m.speed + m.phase));
        ctx.fillStyle = `hsla(${m.hue}, 60%, 70%, ${a})`;
        ctx.beginPath();
        ctx.arc(m.x * W, m.y * H, m.r, 0, Math.PI * 2);
        ctx.fill();
      }
      raf = requestAnimationFrame(draw);
    };
    draw();
    return () => { cancelAnimationFrame(raf); window.removeEventListener("resize", resize); };
  }, []);
  return <canvas ref={ref} className={cn("absolute inset-0 w-full h-full pointer-events-none", className)} />;
}

/* ─── Scroll-triggered reveal ─── */
function Reveal({ children, className, delay = 0 }: { children: React.ReactNode; className?: string; delay?: number }) {
  const ref = useRef<HTMLDivElement>(null);
  const [vis, setVis] = useState(false);
  useEffect(() => {
    const obs = new IntersectionObserver(([e]) => { if (e.isIntersecting) { setVis(true); obs.disconnect(); } }, { threshold: 0.12 });
    obs.observe(ref.current!);
    return () => obs.disconnect();
  }, []);
  return (
    <div ref={ref} className={cn(className)} style={{
      opacity: vis ? 1 : 0,
      transform: vis ? "translateY(0)" : "translateY(32px)",
      transition: `opacity 0.8s cubic-bezier(0.22, 1, 0.36, 1) ${delay}ms, transform 0.8s cubic-bezier(0.22, 1, 0.36, 1) ${delay}ms`,
    }}>{children}</div>
  );
}

/* ─── Animated counter ─── */
function Counter({ end, suffix = "", decimal = false }: { end: number; suffix?: string; decimal?: boolean }) {
  const [val, setVal] = useState(0);
  const ref = useRef<HTMLSpanElement>(null);
  useEffect(() => {
    const el = ref.current!;
    const obs = new IntersectionObserver(([e]) => {
      if (!e.isIntersecting) return;
      obs.disconnect();
      const start = performance.now();
      const dur = 1800;
      const tick = (now: number) => {
        const p = Math.min((now - start) / dur, 1);
        const eased = 1 - Math.pow(1 - p, 3);
        setVal(decimal ? parseFloat((end * eased).toFixed(1)) : Math.floor(end * eased));
        if (p < 1) requestAnimationFrame(tick);
      };
      requestAnimationFrame(tick);
    }, { threshold: 0.3 });
    obs.observe(el);
    return () => obs.disconnect();
  }, [end, decimal]);
  return <span ref={ref}>{val}{suffix}</span>;
}

/* ─── Section label ─── */
function SectionTag({ children }: { children: string }) {
  return (
    <span className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-[11px] font-semibold tracking-[0.2em] uppercase"
      style={{ background: "rgba(139,92,246,0.1)", color: "#A78BFA", border: "1px solid rgba(139,92,246,0.2)" }}>
      <span className="w-1.5 h-1.5 rounded-full bg-violet-400 animate-pulse" />
      {children}
    </span>
  );
}

/* ══════════════════════════════════════════════════════════════════
   NAV
   ══════════════════════════════════════════════════════════════════ */
function Nav() {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const h = () => setScrolled(window.scrollY > 60);
    window.addEventListener("scroll", h, { passive: true });
    return () => window.removeEventListener("scroll", h);
  }, []);
  return (
    <nav className={cn(
      "fixed top-0 inset-x-0 z-50 transition-all duration-500",
      scrolled ? "py-3" : "py-5",
    )} style={{
      background: scrolled ? "rgba(8,8,12,0.85)" : "transparent",
      backdropFilter: scrolled ? "blur(20px) saturate(1.4)" : "none",
      borderBottom: scrolled ? "1px solid rgba(139,92,246,0.1)" : "1px solid transparent",
    }}>
      <div className="max-w-7xl mx-auto flex items-center justify-between px-6">
        <a href="#hero" className="flex items-center gap-2.5 group">
          <div className="w-9 h-9 rounded-lg flex items-center justify-center font-display font-extrabold text-sm text-white transition-transform group-hover:scale-110"
            style={{ background: "linear-gradient(135deg, #8B5CF6, #6D28D9)" }}>
            R
          </div>
          <span className="font-display font-bold text-[17px] tracking-tight text-white/90">RhythmForge</span>
        </a>

        <div className="hidden md:flex items-center gap-8 text-[13px] font-medium text-white/40">
          {[["#features","Ozellikler"],["#gameplay","Oynanis"],["#how","Nasil"],["#download","Indir"]].map(([href,label]) => (
            <a key={href} href={href} className="hover:text-violet-300 transition-colors duration-300">{label}</a>
          ))}
        </div>

        <a href="#download" className="px-5 py-2 rounded-lg text-[13px] font-semibold text-white transition-all duration-300 hover:shadow-[0_0_24px_rgba(139,92,246,0.3)]"
          style={{ background: "linear-gradient(135deg, #7C3AED, #6D28D9)", border: "1px solid rgba(167,139,250,0.3)" }}>
          Indir
        </a>
      </div>
    </nav>
  );
}

/* ══════════════════════════════════════════════════════════════════
   HERO
   ══════════════════════════════════════════════════════════════════ */
function Hero() {
  return (
    <section id="hero" className="relative min-h-screen flex flex-col items-center justify-center overflow-hidden">
      {/* Multi-layer background */}
      <div className="absolute inset-0" style={{ background: "radial-gradient(ellipse 80% 60% at 50% 40%, #1a0d2e 0%, #08080C 70%)" }} />
      <div className="absolute inset-0" style={{ background: "radial-gradient(circle at 20% 80%, rgba(139,92,246,0.08) 0%, transparent 40%)" }} />
      <div className="absolute inset-0" style={{ background: "radial-gradient(circle at 80% 20%, rgba(245,158,11,0.05) 0%, transparent 40%)" }} />

      {/* Frequency visualizer at bottom */}
      <div className="absolute bottom-0 left-0 right-0 h-[35vh] opacity-40">
        <FrequencyCanvas />
      </div>

      {/* Subtle grid lines */}
      <div className="absolute inset-0 opacity-[0.03]"
        style={{ backgroundImage: "linear-gradient(rgba(139,92,246,0.5) 1px, transparent 1px), linear-gradient(90deg, rgba(139,92,246,0.5) 1px, transparent 1px)", backgroundSize: "60px 60px" }} />

      {/* Grain overlay */}
      <div className="absolute inset-0 opacity-[0.035] pointer-events-none"
        style={{ backgroundImage: "url(\"data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='1'/%3E%3C/svg%3E\")" }} />

      <DriftCanvas className="opacity-60" />

      {/* Diagonal accent line */}
      <div className="absolute top-[15%] -left-20 w-[140%] h-px opacity-10"
        style={{ background: "linear-gradient(90deg, transparent, #8B5CF6, transparent)", transform: "rotate(-3deg)" }} />

      <div className="relative z-10 text-center px-6 max-w-5xl">
        <Reveal>
          <SectionTag>Season 1 — Atesin Ritmi</SectionTag>
        </Reveal>

        <Reveal delay={120}>
          <h1 className="mt-8 font-display font-extrabold tracking-[-0.04em] leading-[0.88] text-white"
            style={{ fontSize: "clamp(3rem, 8vw, 7rem)" }}>
            Ritmi{" "}
            <span className="bg-clip-text text-transparent" style={{ backgroundImage: "linear-gradient(135deg, #A78BFA, #7C3AED, #6D28D9)" }}>
              Hisset
            </span>
            <br />
            Sahneyi{" "}
            <span className="bg-clip-text text-transparent" style={{ backgroundImage: "linear-gradient(135deg, #F59E0B, #D97706, #B45309)" }}>
              Fethet
            </span>
          </h1>
        </Reveal>

        <Reveal delay={260}>
          <p className="mt-7 text-[17px] sm:text-lg leading-relaxed max-w-xl mx-auto" style={{ color: "#8B8BA7" }}>
            Kendi muziklerini yukle, AI ritim haritasi olusturken bekle,
            sonra 5 seritli highway'de alevli bir performans sergile.
          </p>
        </Reveal>

        <Reveal delay={400}>
          <div className="mt-10 flex flex-col sm:flex-row gap-4 justify-center">
            <a href="#download" className="group relative px-8 py-4 rounded-xl font-semibold text-[15px] text-white overflow-hidden transition-all duration-300 hover:shadow-[0_0_40px_rgba(139,92,246,0.35)] hover:scale-[1.02]"
              style={{ background: "linear-gradient(135deg, #7C3AED, #6D28D9)" }}>
              <span className="relative z-10 flex items-center justify-center gap-2.5">
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24"><path d="M17.523 2.237a.625.625 0 0 0-.803.368l-1.456 3.72a8.167 8.167 0 0 0-6.528 0L7.28 2.605a.625.625 0 1 0-1.17.435l1.358 3.47A8.2 8.2 0 0 0 3.5 13.25h17a8.2 8.2 0 0 0-3.968-6.74l1.358-3.47a.625.625 0 0 0-.367-.803M8.5 10.75a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5m7 0a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5M3.5 14.5v5a2.5 2.5 0 0 0 2.5 2.5h1V14.5zm14.5 0v7.5h1a2.5 2.5 0 0 0 2.5-2.5v-5zM7.25 14.5v8h4V14.5zm5 0v8h4.25V14.5z"/></svg>
                Hemen Indir
              </span>
            </a>
            <a href="#gameplay" className="px-8 py-4 rounded-xl font-semibold text-[15px] text-white/70 transition-all duration-300 hover:text-white hover:border-violet-500/40 flex items-center justify-center gap-2.5"
              style={{ border: "1px solid rgba(255,255,255,0.1)", background: "rgba(255,255,255,0.03)" }}>
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M14.752 11.168l-3.197-2.132A1 1 0 0 0 10 9.87v4.263a1 1 0 0 0 1.555.832l3.197-2.132a1 1 0 0 0 0-1.664z" /><path strokeLinecap="round" strokeLinejoin="round" d="M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z" /></svg>
              Nasil Oynanir
            </a>
          </div>
        </Reveal>
      </div>

      {/* Scroll indicator */}
      <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2">
        <div className="w-5 h-8 rounded-full border border-white/15 flex justify-center pt-1.5">
          <div className="w-1 h-2 rounded-full bg-violet-400/60 animate-bounce" />
        </div>
      </div>
    </section>
  );
}

/* ══════════════════════════════════════════════════════════════════
   FEATURES — Bento Grid
   ══════════════════════════════════════════════════════════════════ */
function Features() {
  const features = [
    { icon: "🎸", title: "Kendi Muzigini Yukle", desc: "MP3, WAV, OGG veya FLAC — muzik kutuphanenden istedigin sarkiyi yukle. AI analiz motoru ritim haritasini saniyeler icinde olusturur.", span: "sm:col-span-2" },
    { icon: "🎤", title: "Ses Kaydi", desc: "Mikrofon ile kayit yap, dalga formunu gorsel olarak duzenle, kirp ve aninda oynanabilir parca olustur.", span: "" },
    { icon: "🎯", title: "5 Seritli Highway", desc: "Frekans tabanli 5 serit: bas gitardan tiz gitara. Perspektif highway uzerinde notalar akar.", span: "" },
    { icon: "🔥", title: "Combo × Power", desc: "10+ combo = 2x, 30+ = 3x, 50+ = 4x. Power aktifken carpanlar catlanir: 4x combo × 4x power = 16x puan!", span: "sm:col-span-2" },
    { icon: "🏆", title: "Zorluk Seviyeleri", desc: "Kolay (3 serit), Orta (5 serit), Zor (tum notalar + akorlar). S derecesini hedefle.", span: "" },
    { icon: "🎨", title: "Tema Magazasi", desc: "Kozmik Mor, Sahne Rock, ates efektleri. Oyun icinde kazandigin paralarla kisisellestirebilirsin.", span: "" },
  ];

  return (
    <section id="features" className="relative py-28 overflow-hidden" style={{ background: "#08080C" }}>
      <div className="absolute inset-0" style={{ background: "radial-gradient(ellipse at top, rgba(139,92,246,0.06) 0%, transparent 50%)" }} />

      <div className="relative z-10 max-w-6xl mx-auto px-6">
        <Reveal>
          <div className="text-center mb-16">
            <SectionTag>Ozellikler</SectionTag>
            <h2 className="mt-5 font-display font-extrabold text-4xl sm:text-5xl tracking-[-0.03em] text-white">
              Her Notu Hisset
            </h2>
            <p className="mt-4 max-w-lg mx-auto text-[15px] leading-relaxed" style={{ color: "#6B6780" }}>
              Yapay zeka destekli analiz motoru her nota ve vurusu yakalayarak sana ozel harita olusturur.
            </p>
          </div>
        </Reveal>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {features.map((f, i) => (
            <Reveal key={i} delay={i * 80} className={cn("group", f.span)}>
              <div className="relative h-full rounded-2xl p-6 transition-all duration-500 hover:translate-y-[-2px]"
                style={{
                  background: "linear-gradient(135deg, rgba(139,92,246,0.06), rgba(139,92,246,0.02))",
                  border: "1px solid rgba(139,92,246,0.08)",
                }}>
                {/* Hover glow */}
                <div className="absolute inset-0 rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500"
                  style={{ background: "radial-gradient(ellipse at center, rgba(139,92,246,0.08) 0%, transparent 70%)" }} />
                <div className="relative z-10">
                  <div className="text-3xl mb-4">{f.icon}</div>
                  <h3 className="text-[17px] font-display font-bold text-white mb-2">{f.title}</h3>
                  <p className="text-[13px] leading-relaxed" style={{ color: "#6B6780" }}>{f.desc}</p>
                </div>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ══════════════════════════════════════════════════════════════════
   GAMEPLAY MOCKUP
   ══════════════════════════════════════════════════════════════════ */
function Gameplay() {
  const laneColors = ["#50E1F9", "#C47FFF", "#FFE792", "#4ADE80", "#FF6B6B"];

  return (
    <section id="gameplay" className="relative py-28 overflow-hidden" style={{ background: "linear-gradient(180deg, #08080C 0%, #0d0a1a 50%, #08080C 100%)" }}>
      <DriftCanvas className="opacity-30" />
      <div className="absolute inset-0" style={{ background: "radial-gradient(ellipse at bottom center, rgba(124,58,237,0.12) 0%, transparent 60%)" }} />

      <div className="relative z-10 max-w-6xl mx-auto px-6">
        <Reveal>
          <div className="text-center mb-16">
            <SectionTag>Oynanis</SectionTag>
            <h2 className="mt-5 font-display font-extrabold text-4xl sm:text-5xl tracking-[-0.03em] text-white">
              Kozmik Highway
            </h2>
            <p className="mt-4 max-w-xl mx-auto text-[15px] leading-relaxed" style={{ color: "#6B6780" }}>
              3D perspektifli highway uzerinde notalar sana dogru akar. Zamanlama her sey.
            </p>
          </div>
        </Reveal>

        <Reveal delay={200}>
          <div className="relative max-w-3xl mx-auto"
            style={{ perspective: "1200px" }}>
            <div className="relative aspect-video rounded-2xl overflow-hidden"
              style={{
                border: "1px solid rgba(139,92,246,0.15)",
                boxShadow: "0 0 80px rgba(139,92,246,0.12), 0 20px 60px rgba(0,0,0,0.5)",
                transform: "rotateX(2deg)",
              }}>
              <div className="absolute inset-0" style={{ background: "linear-gradient(180deg, #0d0020 0%, #1a0040 100%)" }} />

              {/* Highway perspective */}
              <svg className="absolute inset-0 w-full h-full" viewBox="0 0 800 450" preserveAspectRatio="none">
                {/* Highway body */}
                <polygon points="320,50 480,50 650,420 150,420" fill="rgba(80,40,150,0.12)" stroke="rgba(139,92,246,0.15)" strokeWidth="1" />
                {/* Lane dividers */}
                {[1,2,3,4].map(i => {
                  const topX = 320 + (480-320) * i / 5;
                  const botX = 150 + (650-150) * i / 5;
                  return <line key={i} x1={topX} y1={50} x2={botX} y2={420} stroke="rgba(139,92,246,0.06)" strokeWidth="1" />;
                })}
                {/* Strike line */}
                <line x1="150" y1="380" x2="650" y2="380" stroke="#50E1F9" strokeWidth="2" opacity="0.7" />
                {/* Speed lines */}
                {[100,160,220,280,340].map((y,i) => {
                  const p = y/420;
                  const lx = 320 + (150-320)*p;
                  const rx = 480 + (650-480)*p;
                  return <line key={i} x1={lx} y1={y} x2={rx} y2={y} stroke={`rgba(139,92,246,${0.03+p*0.04})`} strokeWidth="1" />;
                })}
              </svg>

              {/* Animated notes */}
              {[
                { lane: 0, y: "72%", s: 28 },
                { lane: 1, y: "55%", s: 22 },
                { lane: 2, y: "35%", s: 16 },
                { lane: 3, y: "22%", s: 11 },
                { lane: 4, y: "78%", s: 30 },
              ].map((n, i) => {
                const positions = ["22%","36%","50%","64%","78%"];
                const c = laneColors[n.lane];
                return (
                  <div key={i} className="absolute rounded-full animate-pulse" style={{
                    left: positions[n.lane], top: n.y,
                    width: n.s, height: n.s,
                    background: `radial-gradient(circle, white 0%, ${c} 40%, transparent 70%)`,
                    boxShadow: `0 0 ${n.s*1.5}px ${c}66`,
                    transform: "translate(-50%, -50%)",
                  }} />
                );
              })}

              {/* Fret buttons */}
              <div className="absolute bottom-[8%] left-1/2 -translate-x-1/2 flex gap-[3%] w-[60%] justify-center">
                {laneColors.map((c, i) => (
                  <div key={i} className="w-10 h-10 sm:w-12 sm:h-12 rounded-full flex items-center justify-center" style={{
                    border: `2px solid ${c}`,
                    background: `radial-gradient(circle, ${c}22, ${c}08)`,
                    boxShadow: `0 0 12px ${c}33`,
                  }}>
                    <div className="w-3 h-3 sm:w-4 sm:h-4 rounded-full" style={{ background: c }} />
                  </div>
                ))}
              </div>

              {/* HUD */}
              <div className="absolute top-3 right-4 text-right">
                <div className="text-[10px] tracking-[0.15em] uppercase font-medium" style={{ color: "#6B6780" }}>Score</div>
                <div className="text-white font-display font-bold text-xl tabular-nums">12,480</div>
                <div className="text-amber-400 font-display font-bold text-sm">4x</div>
              </div>

              {/* Combo */}
              <div className="absolute top-3 left-4">
                <div className="text-[10px] tracking-[0.15em] uppercase font-medium" style={{ color: "#6B6780" }}>Combo</div>
                <div className="text-violet-300 font-display font-bold text-lg">47</div>
              </div>
            </div>
          </div>
        </Reveal>

        {/* Hit windows */}
        <div className="mt-16 grid sm:grid-cols-3 gap-4 max-w-2xl mx-auto">
          {[
            { label: "Perfect", range: "±100ms", color: "#F59E0B" },
            { label: "Great", range: "±200ms", color: "#4ADE80" },
            { label: "Good", range: "±300ms", color: "#50E1F9" },
          ].map((h, i) => (
            <Reveal key={i} delay={i * 100}>
              <div className="text-center p-5 rounded-xl" style={{
                background: `linear-gradient(135deg, ${h.color}08, ${h.color}03)`,
                border: `1px solid ${h.color}20`,
              }}>
                <div className="text-xl font-display font-bold" style={{ color: h.color }}>{h.label}</div>
                <div className="text-[13px] mt-1" style={{ color: "#6B6780" }}>{h.range}</div>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ══════════════════════════════════════════════════════════════════
   MULTIPLIER SYSTEM — Visual explanation of combo × power
   ══════════════════════════════════════════════════════════════════ */
function MultiplierSystem() {
  return (
    <section className="relative py-28 overflow-hidden" style={{ background: "#08080C" }}>
      <div className="absolute inset-0" style={{ background: "radial-gradient(ellipse at center, rgba(245,158,11,0.04) 0%, transparent 50%)" }} />

      <div className="relative z-10 max-w-5xl mx-auto px-6">
        <Reveal>
          <div className="text-center mb-16">
            <SectionTag>Skor Sistemi</SectionTag>
            <h2 className="mt-5 font-display font-extrabold text-4xl sm:text-5xl tracking-[-0.03em] text-white">
              Combo × Power
            </h2>
            <p className="mt-4 max-w-xl mx-auto text-[15px] leading-relaxed" style={{ color: "#6B6780" }}>
              Combo kademesi ile Power carpanini carp — maksimum 16x puana ulas.
            </p>
          </div>
        </Reveal>

        {/* Combo tiers */}
        <Reveal delay={100}>
          <div className="grid grid-cols-4 gap-3 mb-8">
            {[
              { combo: "0–9", mult: "1x", color: "#6B6780", bg: "rgba(107,103,128,0.06)" },
              { combo: "10–29", mult: "2x", color: "#50E1F9", bg: "rgba(80,225,249,0.06)" },
              { combo: "30–49", mult: "3x", color: "#C47FFF", bg: "rgba(196,127,255,0.06)" },
              { combo: "50+", mult: "4x", color: "#F59E0B", bg: "rgba(245,158,11,0.06)" },
            ].map((tier, i) => (
              <div key={i} className="text-center p-4 sm:p-6 rounded-xl transition-all duration-300 hover:scale-[1.03]"
                style={{ background: tier.bg, border: `1px solid ${tier.color}18` }}>
                <div className="font-display font-extrabold text-2xl sm:text-3xl" style={{ color: tier.color }}>{tier.mult}</div>
                <div className="text-[11px] sm:text-xs mt-2 font-medium" style={{ color: "#6B6780" }}>{tier.combo} combo</div>
              </div>
            ))}
          </div>
        </Reveal>

        {/* Multiplication examples */}
        <Reveal delay={200}>
          <div className="rounded-2xl p-6 sm:p-8" style={{ background: "rgba(139,92,246,0.04)", border: "1px solid rgba(139,92,246,0.1)" }}>
            <div className="text-center mb-6">
              <span className="text-[13px] font-semibold" style={{ color: "#A78BFA" }}>Power Aktifken</span>
            </div>
            <div className="grid sm:grid-cols-3 gap-4">
              {[
                { combo: "1x", power: "4x", total: "4x", highlight: false },
                { combo: "2x", power: "4x", total: "8x", highlight: false },
                { combo: "4x", power: "4x", total: "16x", highlight: true },
              ].map((ex, i) => (
                <div key={i} className="flex items-center justify-center gap-3 py-4 px-3 rounded-xl" style={{
                  background: ex.highlight ? "rgba(245,158,11,0.08)" : "rgba(255,255,255,0.02)",
                  border: ex.highlight ? "1px solid rgba(245,158,11,0.2)" : "1px solid rgba(255,255,255,0.05)",
                }}>
                  <span className="font-display font-bold text-lg text-white/60">{ex.combo}</span>
                  <span className="text-violet-400 text-sm">×</span>
                  <span className="font-display font-bold text-lg text-violet-300">{ex.power}</span>
                  <span className="text-white/30">=</span>
                  <span className="font-display font-extrabold text-xl" style={{ color: ex.highlight ? "#F59E0B" : "#A78BFA" }}>{ex.total}</span>
                </div>
              ))}
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

/* ══════════════════════════════════════════════════════════════════
   STATS
   ══════════════════════════════════════════════════════════════════ */
function Stats() {
  const stats = [
    { value: 5, suffix: "", label: "Serit", decimal: false },
    { value: 3, suffix: "", label: "Zorluk", decimal: false },
    { value: 16, suffix: "x", label: "Max Carpan", decimal: false },
    { value: 60, suffix: "", label: "FPS Render", decimal: false },
  ];
  return (
    <section className="relative py-20 overflow-hidden" style={{ background: "#08080C" }}>
      <div className="relative z-10 max-w-5xl mx-auto px-6">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-8">
          {stats.map((s, i) => (
            <Reveal key={i} delay={i * 80}>
              <div className="text-center">
                <div className="font-display font-extrabold text-4xl sm:text-5xl bg-clip-text text-transparent"
                  style={{ backgroundImage: "linear-gradient(135deg, #A78BFA, #7C3AED)" }}>
                  <Counter end={s.value} suffix={s.suffix} decimal={s.decimal} />
                </div>
                <div className="text-[11px] mt-2 uppercase tracking-[0.15em] font-medium" style={{ color: "#4A4662" }}>{s.label}</div>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ══════════════════════════════════════════════════════════════════
   HOW IT WORKS
   ══════════════════════════════════════════════════════════════════ */
function HowItWorks() {
  const steps = [
    { num: "01", title: "Sarki Yukle", desc: "Telefonundan MP3, WAV, OGG veya FLAC formatinda muzik dosyani sec veya mikrofon ile kayit yap.", accent: "#A78BFA" },
    { num: "02", title: "AI Analiz", desc: "Yapay zeka motoru tempoyu, frekans bandlarini ve vuruslari tespit eder. Saniyeler icinde ritim haritasi hazir.", accent: "#F59E0B" },
    { num: "03", title: "Oyna & Fethet", desc: "Zorluk seviyeni sec, highway'e atil. Perfect isabetlerle combo zincirini kur ve en yuksek skoru yakala!", accent: "#FF6B6B" },
  ];
  return (
    <section id="how" className="relative py-28 overflow-hidden" style={{ background: "linear-gradient(180deg, #08080C, #0a0815, #08080C)" }}>
      <div className="relative z-10 max-w-3xl mx-auto px-6">
        <Reveal>
          <div className="text-center mb-16">
            <SectionTag>Nasil Calisir</SectionTag>
            <h2 className="mt-5 font-display font-extrabold text-4xl sm:text-5xl tracking-[-0.03em] text-white">
              Uc Adimda Basla
            </h2>
          </div>
        </Reveal>

        <div className="space-y-6">
          {steps.map((s, i) => (
            <Reveal key={i} delay={i * 120}>
              <div className="flex gap-5 items-start group">
                <div className="flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center font-display font-extrabold text-sm text-white/90 transition-all group-hover:scale-110"
                  style={{ background: `${s.accent}18`, border: `1px solid ${s.accent}25`, color: s.accent }}>
                  {s.num}
                </div>
                <div className="pt-1">
                  <h3 className="text-lg font-display font-bold text-white mb-1">{s.title}</h3>
                  <p className="text-[14px] leading-relaxed" style={{ color: "#6B6780" }}>{s.desc}</p>
                </div>
              </div>
              {i < steps.length - 1 && (
                <div className="ml-6 w-px h-6" style={{ background: "linear-gradient(180deg, rgba(139,92,246,0.15), transparent)" }} />
              )}
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ══════════════════════════════════════════════════════════════════
   DIFFICULTY
   ══════════════════════════════════════════════════════════════════ */
function Difficulty() {
  const diffs = [
    { name: "Kolay", lanes: 3, reduction: "40%", grade: "Baslangiç", color: "#4ADE80" },
    { name: "Orta", lanes: 5, reduction: "20%", grade: "Oyuncu", color: "#F59E0B" },
    { name: "Zor", lanes: 5, reduction: "0%", grade: "Usta", color: "#FF6B6B" },
  ];
  return (
    <section className="relative py-28 overflow-hidden" style={{ background: "#08080C" }}>
      <div className="relative z-10 max-w-5xl mx-auto px-6">
        <Reveal>
          <div className="text-center mb-16">
            <SectionTag>Zorluk</SectionTag>
            <h2 className="mt-5 font-display font-extrabold text-4xl sm:text-5xl tracking-[-0.03em] text-white">
              Seviyeni Sec
            </h2>
          </div>
        </Reveal>

        <div className="grid sm:grid-cols-3 gap-4">
          {diffs.map((d, i) => (
            <Reveal key={i} delay={i * 120}>
              <div className="rounded-2xl p-6 transition-all duration-300 hover:translate-y-[-2px]"
                style={{ background: `${d.color}06`, border: `1px solid ${d.color}15` }}>
                <div className="flex items-center justify-between mb-5">
                  <span className="px-3 py-1 rounded-full text-[11px] font-bold uppercase tracking-wider"
                    style={{ background: `${d.color}15`, color: d.color }}>
                    {d.name}
                  </span>
                  <span className="text-[11px]" style={{ color: "#4A4662" }}>{d.grade}</span>
                </div>

                <div className="space-y-3">
                  {[
                    ["Serit", `${d.lanes}`],
                    ["Not Azaltma", d.reduction],
                    ["Akorlar", d.name === "Zor" ? "Evet" : "Hayir"],
                  ].map(([label, value]) => (
                    <div key={label} className="flex justify-between text-[13px]">
                      <span style={{ color: "#4A4662" }}>{label}</span>
                      <span className="font-semibold text-white/80">{value}</span>
                    </div>
                  ))}
                </div>

                {/* Lane vis */}
                <div className="mt-5 flex gap-1 justify-center">
                  {[0,1,2,3,4].map(l => (
                    <div key={l} className="rounded transition-all" style={{
                      height: 28,
                      width: l < d.lanes ? 20 : 8,
                      background: l < d.lanes
                        ? `linear-gradient(180deg, ${d.color}30, ${d.color}10)`
                        : "rgba(255,255,255,0.04)",
                    }} />
                  ))}
                </div>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ══════════════════════════════════════════════════════════════════
   GRADES
   ══════════════════════════════════════════════════════════════════ */
function Grades() {
  const grades = [
    { grade: "S", min: "95%+", color: "#F59E0B" },
    { grade: "A", min: "85%+", color: "#FB923C" },
    { grade: "B", min: "75%+", color: "#A78BFA" },
    { grade: "C", min: "65%+", color: "#50E1F9" },
    { grade: "D", min: "<65%", color: "#4A4662" },
  ];
  return (
    <section className="relative py-20" style={{ background: "#08080C" }}>
      <div className="max-w-3xl mx-auto px-6">
        <Reveal>
          <div className="text-center mb-12">
            <SectionTag>Derecelendirme</SectionTag>
            <h2 className="mt-5 font-display font-extrabold text-3xl sm:text-4xl tracking-[-0.03em] text-white">
              Derece Sistemi
            </h2>
          </div>
        </Reveal>

        <Reveal delay={150}>
          <div className="flex flex-wrap justify-center gap-3">
            {grades.map((g, i) => (
              <div key={i} className="flex flex-col items-center p-4 rounded-xl min-w-[72px] transition-all duration-300 hover:translate-y-[-2px]"
                style={{
                  background: `${g.color}08`,
                  border: `1px solid ${g.color}15`,
                  boxShadow: i === 0 ? `0 0 20px ${g.color}10` : "none",
                }}>
                <span className="font-display font-extrabold text-2xl" style={{ color: g.color }}>{g.grade}</span>
                <span className="text-[11px] mt-1" style={{ color: "#4A4662" }}>{g.min}</span>
              </div>
            ))}
          </div>
        </Reveal>
      </div>
    </section>
  );
}

/* ══════════════════════════════════════════════════════════════════
   DOWNLOAD CTA
   ══════════════════════════════════════════════════════════════════ */
function Download() {
  return (
    <section id="download" className="relative py-32 overflow-hidden">
      <div className="absolute inset-0" style={{ background: "radial-gradient(ellipse at center, #1a0d2e 0%, #08080C 60%)" }} />
      <DriftCanvas className="opacity-40" />

      {/* Diagonal accent lines */}
      <div className="absolute top-[30%] -left-20 w-[140%] h-px opacity-[0.06]"
        style={{ background: "linear-gradient(90deg, transparent, #A78BFA, transparent)", transform: "rotate(-2deg)" }} />
      <div className="absolute top-[70%] -left-20 w-[140%] h-px opacity-[0.04]"
        style={{ background: "linear-gradient(90deg, transparent, #F59E0B, transparent)", transform: "rotate(1deg)" }} />

      <div className="relative z-10 max-w-3xl mx-auto px-6 text-center">
        <Reveal>
          <SectionTag>Ucretsiz</SectionTag>
        </Reveal>

        <Reveal delay={100}>
          <h2 className="mt-6 font-display font-extrabold tracking-[-0.04em] text-white"
            style={{ fontSize: "clamp(2.5rem, 6vw, 4.5rem)" }}>
            Sahneye{" "}
            <span className="bg-clip-text text-transparent" style={{ backgroundImage: "linear-gradient(135deg, #A78BFA, #7C3AED)" }}>
              Cik
            </span>
          </h2>
        </Reveal>

        <Reveal delay={200}>
          <p className="mt-5 text-[16px] max-w-md mx-auto leading-relaxed" style={{ color: "#6B6780" }}>
            Android cihazina simdi indir. Kendi muziklerin, kendi ritim haritalarinla sinirsiz eglence.
          </p>
        </Reveal>

        <Reveal delay={300}>
          <a href="#" className="mt-10 inline-flex items-center gap-3 px-10 py-5 rounded-2xl font-display font-bold text-lg text-white transition-all duration-300 hover:shadow-[0_0_50px_rgba(139,92,246,0.3)] hover:scale-[1.03]"
            style={{ background: "linear-gradient(135deg, #7C3AED, #6D28D9)", border: "1px solid rgba(167,139,250,0.3)" }}>
            <svg className="w-7 h-7" viewBox="0 0 24 24" fill="currentColor">
              <path d="M17.523 2.237a.625.625 0 0 0-.803.368l-1.456 3.72a8.167 8.167 0 0 0-6.528 0L7.28 2.605a.625.625 0 1 0-1.17.435l1.358 3.47A8.2 8.2 0 0 0 3.5 13.25h17a8.2 8.2 0 0 0-3.968-6.74l1.358-3.47a.625.625 0 0 0-.367-.803M8.5 10.75a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5m7 0a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5M3.5 14.5v5a2.5 2.5 0 0 0 2.5 2.5h1V14.5zm14.5 0v7.5h1a2.5 2.5 0 0 0 2.5-2.5v-5zM7.25 14.5v8h4V14.5zm5 0v8h4.25V14.5z" />
            </svg>
            Android Indir
          </a>
        </Reveal>

        <Reveal delay={400}>
          <p className="mt-5 text-[12px]" style={{ color: "#32304A" }}>Android 8.0+ (API 26) gerektirir</p>
        </Reveal>
      </div>
    </section>
  );
}

/* ══════════════════════════════════════════════════════════════════
   FOOTER
   ══════════════════════════════════════════════════════════════════ */
function Footer() {
  return (
    <footer className="py-10" style={{ background: "#06060A", borderTop: "1px solid rgba(139,92,246,0.06)" }}>
      <div className="max-w-6xl mx-auto px-6 flex flex-col sm:flex-row items-center justify-between gap-6">
        <div className="flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-md flex items-center justify-center font-display font-extrabold text-xs text-white"
            style={{ background: "linear-gradient(135deg, #7C3AED, #6D28D9)" }}>
            R
          </div>
          <span className="font-display font-bold text-[15px] text-white/80">RhythmForge</span>
        </div>
        <div className="flex items-center gap-6 text-[12px] font-medium" style={{ color: "#32304A" }}>
          <a href="#features" className="hover:text-violet-400 transition-colors">Ozellikler</a>
          <a href="#gameplay" className="hover:text-violet-400 transition-colors">Oynanis</a>
          <a href="#download" className="hover:text-violet-400 transition-colors">Indir</a>
        </div>
        <p className="text-[11px]" style={{ color: "#1E1C30" }}>2025 RhythmForge. Tum haklar saklidir.</p>
      </div>
    </footer>
  );
}

/* ══════════════════════════════════════════════════════════════════
   APP
   ══════════════════════════════════════════════════════════════════ */
export default function App() {
  return (
    <div className="min-h-screen antialiased" style={{ background: "#08080C", color: "#E8E4EF", fontFamily: "'Manrope', sans-serif" }}>
      <style>{`
        .font-display { font-family: 'Syne', sans-serif; }
        html { scroll-behavior: smooth; }
        ::selection { background: rgba(139,92,246,0.3); color: #fff; }
        ::-webkit-scrollbar { width: 6px; }
        ::-webkit-scrollbar-track { background: #08080C; }
        ::-webkit-scrollbar-thumb { background: #1E1C30; border-radius: 3px; }
        ::-webkit-scrollbar-thumb:hover { background: #32304A; }
        @keyframes pulse { 50% { opacity: .5; } }
        .animate-pulse { animation: pulse 2s cubic-bezier(0.4,0,0.6,1) infinite; }
        .animate-bounce { animation: bounce 1s infinite; }
        @keyframes bounce { 0%, 100% { transform: translateY(-15%); animation-timing-function: cubic-bezier(0.8,0,1,1); } 50% { transform: none; animation-timing-function: cubic-bezier(0,0,0.2,1); } }
      `}</style>
      <Nav />
      <Hero />
      <Features />
      <Gameplay />
      <MultiplierSystem />
      <Stats />
      <HowItWorks />
      <Difficulty />
      <Grades />
      <Download />
      <Footer />
    </div>
  );
}
