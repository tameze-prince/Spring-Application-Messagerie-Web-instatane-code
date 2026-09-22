"use client";

import { FormEvent, KeyboardEvent, useEffect, useMemo, useRef, useState } from "react";

type IconName = "chat" | "users" | "broadcast" | "bookmark" | "settings" | "search" | "more" | "phone" | "video" | "info" | "smile" | "paperclip" | "mic" | "send" | "close" | "check" | "moon" | "sun" | "command" | "bell" | "arrow" | "palette";

function Icon({ name, size = 20 }: { name: IconName; size?: number }) {
  const paths: Record<IconName, React.ReactNode> = {
    chat: <><path d="M20 15a4 4 0 0 1-4 4H8l-4 3v-7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4h12a4 4 0 0 1 4 4z" /></>,
    users: <><circle cx="9" cy="8" r="3" /><path d="M3 20v-1a6 6 0 0 1 12 0v1M16 4a3 3 0 0 1 0 6M21 20v-1a6 6 0 0 0-4-5.66" /></>,
    broadcast: <><path d="M3 11v2M6 8v8M9 5v14M12 3v18M15 6v12M18 9v6M21 11v2" /></>,
    bookmark: <path d="M6 3h12v18l-6-4-6 4z" />,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.2 2.2-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1.06 1.56V20.4h-3.12v-.1a1.7 1.7 0 0 0-1.06-1.56 1.7 1.7 0 0 0-1.88.34l-.06.06-2.2-2.2.06-.06A1.7 1.7 0 0 0 6.6 15a1.7 1.7 0 0 0-1.56-1.06H5V10.8h.04A1.7 1.7 0 0 0 6.6 9.74a1.7 1.7 0 0 0-.34-1.88L6.2 7.8l2.2-2.2.06.06a1.7 1.7 0 0 0 1.88.34 1.7 1.7 0 0 0 1.06-1.56V4.3h3.12v.14a1.7 1.7 0 0 0 1.06 1.56 1.7 1.7 0 0 0 1.88-.34l.06-.06 2.2 2.2-.06.06a1.7 1.7 0 0 0-.34 1.88 1.7 1.7 0 0 0 1.56 1.06h.14v3.14h-.14A1.7 1.7 0 0 0 19.4 15z" /></>,
    search: <><circle cx="11" cy="11" r="6" /><path d="m20 20-4.35-4.35" /></>,
    more: <><circle cx="5" cy="12" r="1" fill="currentColor" /><circle cx="12" cy="12" r="1" fill="currentColor" /><circle cx="19" cy="12" r="1" fill="currentColor" /></>,
    phone: <path d="M22 16.9v3a2 2 0 0 1-2.18 2 19.8 19.8 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.12 4.2 2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.12.9.34 1.78.65 2.63a2 2 0 0 1-.45 2.11L8.04 9.73a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.85.31 1.73.53 2.63.65A2 2 0 0 1 22 16.9z" />,
    video: <><rect x="3" y="6" width="13" height="12" rx="2" /><path d="m16 10 5-3v10l-5-3z" /></>,
    info: <><circle cx="12" cy="12" r="9" /><path d="M12 11v5M12 8h.01" /></>,
    smile: <><circle cx="12" cy="12" r="9" /><path d="M8 14s1.3 2 4 2 4-2 4-2M9 9h.01M15 9h.01" /></>,
    paperclip: <path d="m20.3 11.7-7.9 7.9a5 5 0 0 1-7.1-7.1l8.1-8.1a3.5 3.5 0 0 1 5 5l-8.1 8.1a2 2 0 0 1-2.8-2.8l7.4-7.4" />,
    mic: <><rect x="9" y="3" width="6" height="11" rx="3" /><path d="M5 11a7 7 0 0 0 14 0M12 18v3M8 21h8" /></>,
    send: <><path d="m21 3-7.5 18-3.7-7.8L3 9.5z" /><path d="m9.8 13.2 5-5" /></>,
    close: <path d="m6 6 12 12M18 6 6 18" />,
    check: <path d="m4 12 4 4L20 5" />,
    moon: <path d="M20 15.5A8 8 0 0 1 8.5 4 8.5 8.5 0 1 0 20 15.5z" />,
    sun: <><circle cx="12" cy="12" r="3.5" /><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" /></>,
    command: <path d="M7.5 7.5a3.5 3.5 0 1 1-3.5 3.5V7.5h3.5Zm9 0a3.5 3.5 0 1 0 3.5 3.5V7.5h-3.5ZM7.5 16.5A3.5 3.5 0 1 0 4 13v3.5h3.5Zm9 0A3.5 3.5 0 1 1 20 13v3.5h-3.5Z" />,
    bell: <><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></>,
    arrow: <path d="m9 18 6-6-6-6" />,
    palette: <><circle cx="12" cy="12" r="9" /><circle cx="8" cy="10" r=".8" fill="currentColor" /><circle cx="12" cy="7" r=".8" fill="currentColor" /><circle cx="16" cy="10" r=".8" fill="currentColor" /><path d="M14 18c0-2 1-3 3-3h3" /></>,
  };
  return <svg className="icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

const chats = [
  { name: "Alma Reyes", initials: "AR", color: "coral", text: "That sounds perfect. I’ll bring the references.", time: "10:42", unread: 2, online: true, pulse: [2,4,2,7,4,8,3,5,2,7,3,2] },
  { name: "Design Sync", initials: "DS", color: "violet", text: "Marek: I’ve added the motion notes", time: "10:36", unread: 0, pulse: [1,2,4,3,2,5,7,4,3,2,4,1] },
  { name: "Noah Bennett", initials: "NB", color: "blue", text: "Voice note", time: "09:58", unread: 0, pulse: [2,3,1,2,3,2,1,2,3,2,1,1] },
  { name: "Weekend plans", initials: "WP", color: "orange", text: "You: Sunday works for me", time: "Yesterday", unread: 0, pulse: [5,3,7,2,4,6,2,3,1,2,3,1] },
  { name: "Saved Messages", initials: "SM", color: "ink", text: "Project links and ideas", time: "Mon", unread: 0, pulse: [1,1,1,2,1,1,1,1,1,1,1,1] },
];

const baseMessages = [
  { from: "them", text: "I’ve been thinking about the direction for the new onboarding flow.", time: "10:31" },
  { from: "them", text: "Less instruction, more momentum. Let people discover the value while they’re already moving through it.", time: "10:32" },
  { from: "me", text: "I love that. Like a good conversation — it shouldn’t feel like a form you have to complete.", time: "10:34", read: true },
  { from: "them", text: "Exactly. I put a few references in the board. The warm, quiet ones felt right.", time: "10:38" },
  { from: "me", text: "That sounds perfect. I’ll bring the references.", time: "10:42", read: true },
];

const frequencies = ["midnight", "daylight", "dusk", "paper", "focus"] as const;
type Frequency = (typeof frequencies)[number];

export default function Messenger() {
  const [active, setActive] = useState(0);
  const [messages, setMessages] = useState(baseMessages);
  const [draft, setDraft] = useState("");
  const [search, setSearch] = useState("");
  const [frequency, setFrequency] = useState<Frequency>("midnight");
  const [density, setDensity] = useState<"compact" | "cozy" | "comfortable">("cozy");
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [detailsOpen, setDetailsOpen] = useState(true);
  const [toast, setToast] = useState<string | null>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => { document.documentElement.dataset.frequency = frequency; }, [frequency]);
  useEffect(() => {
    const onKey = (event: globalThis.KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") { event.preventDefault(); setPaletteOpen(true); }
      if (event.key === "Escape") { setPaletteOpen(false); }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);
  useEffect(() => { if (toast) { const timer = window.setTimeout(() => setToast(null), 4000); return () => window.clearTimeout(timer); } }, [toast]);

  const filteredChats = useMemo(() => chats.filter(c => c.name.toLowerCase().includes(search.toLowerCase()) || c.text.toLowerCase().includes(search.toLowerCase())), [search]);
  const chat = chats[active];
  function sendMessage(event?: FormEvent) {
    event?.preventDefault();
    const text = draft.trim();
    if (!text) return;
    setMessages(prev => [...prev, { from: "me", text, time: "Now", read: false }]);
    setDraft("");
    setToast("Message held for 4 seconds");
    window.setTimeout(() => setMessages(prev => prev.map((m, i) => i === prev.length - 1 ? { ...m, read: true } : m)), 1200);
  }
  function onComposerKey(event: KeyboardEvent<HTMLTextAreaElement>) { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); sendMessage(); } }
  function chooseFrequency(next: Frequency) { setFrequency(next); setPaletteOpen(false); setToast(`${next[0].toUpperCase() + next.slice(1)} frequency active`); }

  return <main className={`messenger density-${density}`}>
    <aside className="app-rail" aria-label="Primary navigation">
      <button className="brand" aria-label="Wavelength home"><span>W</span></button>
      <nav>
        <RailButton icon="chat" label="Chats" active />
        <RailButton icon="users" label="Groups" />
        <RailButton icon="broadcast" label="Channels" />
        <RailButton icon="bookmark" label="Saved messages" />
      </nav>
      <div className="rail-bottom"><RailButton icon="settings" label="Settings" /><button className="avatar avatar-user" aria-label="Open profile">PL</button></div>
    </aside>

    <section className="conversations" aria-label="Conversations">
      <header className="list-header"><div><p className="eyebrow">YOUR SPACE</p><h1>Messages</h1></div><button className="icon-button" aria-label="Open command palette" onClick={() => setPaletteOpen(true)}><Icon name="command" /></button></header>
      <label className="search-box"><Icon name="search" size={18} /><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search conversations" aria-label="Search conversations" /><kbd>⌘ K</kbd></label>
      <div className="section-title"><span>RECENT</span><button onClick={() => setToast("New conversation flow coming next")}>New <span aria-hidden="true">+</span></button></div>
      <div className="chat-list">
        {filteredChats.map((item) => { const originalIndex = chats.indexOf(item); return <button className={`chat-row ${active === originalIndex ? "selected" : ""}`} onClick={() => setActive(originalIndex)} key={item.name}>
          <div className={`avatar avatar-${item.color}`}>{item.initials}{item.online && <i />}</div><div className="chat-main"><div className="chat-name"><strong>{item.name}</strong><time>{item.time}</time></div><div className="chat-preview"><span>{item.text}</span>{item.unread > 0 && <b>{item.unread}</b>}</div></div><Pulse values={item.pulse} /></button>; })}
        {filteredChats.length === 0 && <div className="empty-list">No conversations found.<button onClick={() => setSearch("")}>Clear search</button></div>}
      </div>
      <footer className="focus-status"><Icon name="bell" size={16} /><span>Focus hours until 17:00</span><button aria-label="Configure focus hours"><Icon name="arrow" size={15} /></button></footer>
    </section>

    <section className="chat-panel" aria-label={`Conversation with ${chat.name}`}>
      <header className="chat-header"><div className={`avatar avatar-${chat.color}`}>{chat.initials}{chat.online && <i />}</div><div className="chat-person"><h2>{chat.name}</h2><p>{active === 0 ? "Active now" : "8 members"}</p></div><div className="chat-actions"><button className="icon-button" aria-label="Start voice call"><Icon name="phone" /></button><button className="icon-button" aria-label="Start video call"><Icon name="video" /></button><button className={`icon-button ${detailsOpen ? "active" : ""}`} onClick={() => setDetailsOpen(!detailsOpen)} aria-label="Toggle conversation details"><Icon name="info" /></button></div></header>
      <div className="reconnect" role="status"><span />All caught up. Your messages are in sync.</div>
      <div className="message-scroll">
        <div className="date-divider"><span>Today</span></div>
        {messages.map((message, index) => <article className={`message ${message.from}`} key={`${message.text}-${index}`}><div className="bubble"><p>{message.text}</p><div className="message-meta"><time>{message.time}</time>{message.from === "me" && <span className={message.read ? "receipt read" : "receipt"} aria-label={message.read ? "Read" : "Sent"}><Icon name="check" size={13} /><Icon name="check" size={13} /></span>}</div></div></article>)}
        {draft && <div className="typing" aria-live="polite"><span className="pulse-bars"><i /><i /><i /></span> Alma is typing</div>}
      </div>
      <form className="composer" onSubmit={sendMessage}><button type="button" className="icon-button" aria-label="Attach a file"><Icon name="paperclip" /></button><textarea ref={inputRef} value={draft} onChange={e => setDraft(e.target.value)} onKeyDown={onComposerKey} placeholder="Write a message…" rows={1} aria-label="Message" /><button type="button" className="icon-button" aria-label="Choose an expression"><Icon name="smile" /></button>{draft ? <button className="send-button" aria-label="Send message" type="submit"><Icon name="send" size={19} /></button> : <button type="button" className="icon-button" aria-label="Record voice message"><Icon name="mic" /></button>}</form>
    </section>

    {detailsOpen && <aside className="details" aria-label="Conversation details"><header><button className="close-details" onClick={() => setDetailsOpen(false)} aria-label="Close details"><Icon name="close" /></button><div className={`profile-avatar avatar-${chat.color}`}>{chat.initials}</div><h2>{chat.name}</h2><p>@{chat.name.toLowerCase().replaceAll(" ", ".")}</p></header><div className="detail-actions"><button><Icon name="search" />Search</button><button><Icon name="bell" />Mute</button><button><Icon name="palette" />Frequency</button></div><DetailSection label="SHARED"><button className="media-grid" aria-label="View shared media"><span /><span /><span /><span /><b>View all media <Icon name="arrow" size={15} /></b></button></DetailSection><DetailSection label="ABOUT"><p>Creative director, collector of small beautiful things, usually near a good coffee.</p></DetailSection><DetailSection label="COMFORT"><label className="toggle-row">Read receipts <input type="checkbox" defaultChecked /><span /></label><label className="toggle-row">Mute notifications <input type="checkbox" /><span /></label></DetailSection></aside>}

    <div className="bottom-tabs" aria-label="Mobile navigation"><RailButton icon="chat" label="Chats" active /><RailButton icon="users" label="Groups" /><RailButton icon="settings" label="Settings" /></div>
    {toast && <div className="toast" role="status"><span className="toast-dot" />{toast}<button onClick={() => { setMessages(baseMessages); setToast("Message unsent"); }}>Undo</button></div>}
    {paletteOpen && <CommandPalette onClose={() => setPaletteOpen(false)} onFrequency={chooseFrequency} onSelect={(label) => { setPaletteOpen(false); setToast(label); }} />}
    <div className="frequency-dock" aria-label="Appearance controls"><button className="dock-main" onClick={() => setPaletteOpen(true)}><Icon name="palette" size={17} /> <span>Frequency</span></button><div className="frequency-pips">{frequencies.map(f => <button aria-label={`Switch to ${f}`} className={`pip ${f} ${frequency === f ? "picked" : ""}`} onClick={() => chooseFrequency(f)} key={f} />)}</div><div className="density-control"><span>Density</span>{(["compact", "cozy", "comfortable"] as const).map(d => <button className={density === d ? "active" : ""} onClick={() => setDensity(d)} key={d}>{d.slice(0, 1).toUpperCase()}</button>)}</div></div>
  </main>;
}

function RailButton({ icon, label, active = false }: { icon: IconName; label: string; active?: boolean }) { return <button className={`rail-button ${active ? "active" : ""}`} aria-label={label} aria-current={active ? "page" : undefined}><Icon name={icon} /><span>{label}</span></button>; }
function Pulse({ values }: { values: number[] }) { return <span className="pulse-rail" aria-label="Conversation activity">{values.map((v, i) => <i key={i} style={{ height: `${v * 2}px` }} />)}</span>; }
function DetailSection({ label, children }: { label: string; children: React.ReactNode }) { return <section className="detail-section"><h3>{label}</h3>{children}</section>; }
function CommandPalette({ onClose, onFrequency, onSelect }: { onClose: () => void; onFrequency: (frequency: Frequency) => void; onSelect: (label: string) => void }) { const [query, setQuery] = useState(""); const input = useRef<HTMLInputElement>(null); useEffect(() => input.current?.focus(), []); const actions = [{ label: "New conversation", icon: "chat" as IconName }, { label: "Create a group", icon: "users" as IconName }, { label: "Open appearance studio", icon: "palette" as IconName }, { label: "Settings", icon: "settings" as IconName }].filter(a => a.label.toLowerCase().includes(query.toLowerCase())); return <div className="palette-scrim" role="presentation" onMouseDown={onClose}><section className="command-palette" role="dialog" aria-modal="true" aria-label="Command palette" onMouseDown={e => e.stopPropagation()}><div className="command-input"><Icon name="search" /><input ref={input} value={query} onChange={e => setQuery(e.target.value)} placeholder="Search or run a command" /><kbd>ESC</kbd></div><p className="command-label">QUICK ACTIONS</p>{actions.map(action => <button key={action.label} onClick={() => onSelect(action.label)}><Icon name={action.icon} /><span>{action.label}</span><Icon name="arrow" size={16} /></button>)}<p className="command-label">FREQUENCIES</p><div className="frequency-options">{frequencies.map(f => <button onClick={() => onFrequency(f)} key={f}><span className={`frequency-swatch ${f}`} />{f[0].toUpperCase() + f.slice(1)}</button>)}</div></section></div>; }
