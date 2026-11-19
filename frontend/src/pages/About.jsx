import { Link } from "react-router-dom";

const FEATURES = [
  { icon: "⌕", title: "Discover", text: "Find classmates by skills, interests, branch and batch." },
  { icon: "✦", title: "Collaborate", text: "Share opportunities and build something together." },
  { icon: "✉", title: "Connect", text: "Message people you know in a focused, distraction-free space." },
];

export default function About() {
  return (
    <div className="about-page">
      <div className="page-heading">
        <div>
          <p className="page-eyebrow">ABOUT CONNECT</p>
          <h1>Built for your campus.</h1>
          <p className="page-subtitle">Connect is a networking and collaboration platform made for the VIT-AP student community.</p>
        </div>
        <Link className="page-heading__link" to="/">Sign in <span>→</span></Link>
      </div>
      <section className="about-card">
        <h2>What you can do here</h2>
        <div className="about-grid">
          {FEATURES.map((feature) => (
            <div className="about-feature" key={feature.title}>
              <span className="about-feature__icon" aria-hidden="true">{feature.icon}</span>
              <strong>{feature.title}</strong>
              <p>{feature.text}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
