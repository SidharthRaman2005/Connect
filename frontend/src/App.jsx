import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import ProtectedRoute from "./components/ProtectedRoute";
import AppShell from "./components/AppShell";

const Login = lazy(() => import("./pages/Login"));
const About = lazy(() => import("./pages/About"));
const Dashboard = lazy(() => import("./pages/Dashboard"));
const Onboarding = lazy(() => import("./pages/Onboarding"));
const Profile = lazy(() => import("./pages/Profile"));
const StudentProfile = lazy(() => import("./pages/StudentProfile"));
const Search = lazy(() => import("./pages/Search"));
const Discover = lazy(() => import("./pages/Discover"));
const Connections = lazy(() => import("./pages/Connections"));
const Blog = lazy(() => import("./pages/Blog"));
const Collabs = lazy(() => import("./pages/Collabs"));
const AddCollab = lazy(() => import("./pages/AddCollab"));
const ManageCollabs = lazy(() => import("./pages/ManageCollabs"));
const Chat = lazy(() => import("./pages/Chat"));

function RouteLoading() {
  return <div className="dashboard-loading" role="status"><span className="session-loading__spinner" /> Loading page…</div>;
}

function App() {
  return (
    <Suspense fallback={<RouteLoading />}>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/about" element={<About />} />
        <Route
          path="/onboarding"
          element={<ProtectedRoute><Onboarding /></ProtectedRoute>}
        />
        <Route element={<ProtectedRoute><AppShell /></ProtectedRoute>}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/students/:id" element={<StudentProfile />} />
          <Route path="/search" element={<Search />} />
          <Route path="/discover" element={<Discover />} />
          <Route path="/connections" element={<Connections />} />
          <Route path="/chat" element={<Chat />} />
          <Route path="/blogs" element={<Blog />} />
          <Route path="/collabs" element={<Collabs />} />
          <Route path="/collabs/add" element={<AddCollab />} />
          <Route path="/collabs/manage" element={<ManageCollabs />} />
        </Route>
      </Routes>
    </Suspense>
  );
}

export default App;
