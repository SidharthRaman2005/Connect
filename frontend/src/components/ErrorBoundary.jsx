import { Component } from "react";

/** Keeps one bad render from blanking the entire application. */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error("Unhandled UI error", error, info);
  }

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <main className="error-boundary">
        <div className="error-boundary__card">
          <span className="error-boundary__icon" aria-hidden="true">!</span>
          <h1>Something went wrong</h1>
          <p>The page hit an unexpected error. Reloading usually fixes it.</p>
          <button className="btn btn--primary" type="button" onClick={() => window.location.reload()}>
            Reload Connect
          </button>
        </div>
      </main>
    );
  }
}
