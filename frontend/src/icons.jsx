// Minimal inline icon set (24x24, stroke based) - no external icon dependency.
const PATHS = {
  hat: <><path d="M7 15a4 4 0 1 1 .8-7.9A4.5 4.5 0 0 1 12 4.5a4.5 4.5 0 0 1 4.2 2.6A4 4 0 1 1 17 15" /><path d="M7 15v4.5h10V15" /><path d="M7 17.5h10" /></>,
  dashboard: <><rect x="3.5" y="3.5" width="7" height="7" rx="1.5" /><rect x="13.5" y="3.5" width="7" height="7" rx="1.5" /><rect x="3.5" y="13.5" width="7" height="7" rx="1.5" /><rect x="13.5" y="13.5" width="7" height="7" rx="1.5" /></>,
  calendar: <><rect x="3.5" y="5" width="17" height="15.5" rx="2" /><path d="M3.5 10h17M8 3v4M16 3v4" /></>,
  table: <><path d="M4 9h16M6 9v10M18 9v10M9 5h6" /><path d="M12 5v4" /></>,
  menu: <><path d="M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 1-3-3z" /><path d="M9 8h6M9 12h6" /></>,
  store: <><path d="M4 9l1.5-5h13L20 9" /><path d="M4 9a2.7 2.7 0 0 0 5.3 0 2.7 2.7 0 0 0 5.4 0A2.7 2.7 0 0 0 20 9" /><path d="M5.5 12.5V20h13v-7.5" /></>,
  users: <><circle cx="9" cy="8" r="3.2" /><path d="M3 20c.4-3.3 2.8-5.2 6-5.2s5.6 1.9 6 5.2" /><path d="M16 5.2a3 3 0 0 1 0 5.6M18 14.8c1.8.6 2.9 2.2 3.1 4.2" /></>,
  user: <><circle cx="12" cy="8" r="3.5" /><path d="M5 20c.5-3.7 3.2-5.5 7-5.5s6.5 1.8 7 5.5" /></>,
  bell: <><path d="M6 17V11a6 6 0 1 1 12 0v6l1.5 2h-15z" /><path d="M10 21h4" /></>,
  mail: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3.5 7l8.5 6 8.5-6" /></>,
  logout: <><path d="M10 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4" /><path d="M15 8l4 4-4 4M19 12H9" /></>,
  search: <><circle cx="11" cy="11" r="6.5" /><path d="M16 16l4.5 4.5" /></>,
  clock: <><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></>,
  check: <path d="M5 12.5l4.5 4.5L19 7.5" />,
  x: <path d="M6 6l12 12M18 6L6 18" />,
  plus: <path d="M12 5v14M5 12h14" />,
  minus: <path d="M5 12h14" />,
  left: <path d="M14.5 5.5L8 12l6.5 6.5" />,
  right: <path d="M9.5 5.5L16 12l-6.5 6.5" />,
  down: <path d="M5.5 9.5L12 16l6.5-6.5" />,
  arrow: <path d="M4 12h15M13 6l6 6-6 6" />,
  eye: <><path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" /><circle cx="12" cy="12" r="2.8" /></>,
  pin: <><path d="M12 21s-6.5-5.6-6.5-11a6.5 6.5 0 0 1 13 0c0 5.4-6.5 11-6.5 11z" /><circle cx="12" cy="10" r="2.3" /></>,
  phone: <path d="M5 4h3.5l1.5 4-2 1.5a11 11 0 0 0 5.5 5.5L15 13l4 1.5V18a2 2 0 0 1-2 2A13 13 0 0 1 3 6a2 2 0 0 1 2-2z" />,
  star: <path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.9L12 16.9l-5.2 2.8 1-5.9-4.3-4.1 5.9-.8z" />,
  menuBars: <path d="M4 7h16M4 12h16M4 17h16" />,
  bag: <><path d="M6 8h12l1 12H5z" /><path d="M9 8V6a3 3 0 0 1 6 0v2" /></>,
  trash: <><path d="M4.5 7h15M9 7V4.5h6V7M6.5 7l1 13h9l1-13" /></>,
  edit: <><path d="M4 20l1-4L16.5 4.5a2 2 0 0 1 3 3L8 19z" /></>,
  eyeOff: <><path d="M3 3l18 18" /><path d="M10 6a9 9 0 0 1 2-.5C18 5.5 21.5 12 21.5 12a14 14 0 0 1-3.2 3.9M6.4 7.6A14 14 0 0 0 2.5 12S6 18.5 12 18.5c1.3 0 2.5-.3 3.5-.8" /></>,
  note: <><path d="M5 4h14v12l-5 5H5z" /><path d="M14 21v-5h5M8 9h8M8 13h4" /></>,
};

export default function Icon({ name, size = 20, className = '', ...rest }) {
  return (
    <svg
      className={`icon ${className}`}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      {PATHS[name] || null}
    </svg>
  );
}
