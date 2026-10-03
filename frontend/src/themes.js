// Maps a restaurant's cuisine to a background scene and a sidebar tint.
//
// Each scene is an illustration in /images/art/<key>.svg. To use a real photo instead, drop a file named
// /images/<key>.jpg into frontend/public/images (e.g. grill.jpg, sushi.jpg, italian.jpg, dining.jpg, hero.jpg):
// it is layered on top of the illustration automatically, and the illustration stays as the fallback.
const THEMES = {
  grill: { keywords: ['grill', 'bbq', 'barbecue', 'steak', 'meat', 'african', 'braai', 'brazil', 'burger', 'kebab', 'nyama'], side: ['#2b201c', '#120d0b'] },
  sushi: { keywords: ['japan', 'sushi', 'ramen', 'asia', 'korea', 'izakaya'], side: ['#15244f', '#0a1128'] },
  italian: { keywords: ['ital', 'pizza', 'pasta', 'trattoria', 'mediterr'], side: ['#18432b', '#0b1d13'] },
  dining: { keywords: [], side: ['#2c2724', '#121110'] },
};

export const themeKey = (cuisine = '') => {
  const c = cuisine.toLowerCase();
  return Object.keys(THEMES).find((k) => THEMES[k].keywords.some((w) => c.includes(w))) || 'dining';
};

export const sideColors = (cuisine) => THEMES[themeKey(cuisine)].side;

/** Inline style that paints the cuisine scene (optional photo on top, illustration underneath). */
export const themeStyle = (cuisine) => {
  const k = themeKey(cuisine);
  return { backgroundImage: `url(/images/${k}.jpg), url(/images/art/${k}.svg)` };
};
