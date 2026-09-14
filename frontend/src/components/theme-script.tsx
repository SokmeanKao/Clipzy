/**
 * Blocking theme bootstrap for the root layout (Server Component).
 * Avoids next-themes injecting a <script> inside a Client Component (React 19 warning).
 */
export function ThemeScript() {
  const code = `(function(){try{var t=localStorage.getItem('theme')||'system';var d=t==='system'?(window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light'):t;var r=document.documentElement;r.classList.remove('light','dark');r.classList.add(d);r.style.colorScheme=d;}catch(e){}})();`;

  return (
    <script
      dangerouslySetInnerHTML={{ __html: code }}
      suppressHydrationWarning
    />
  );
}
