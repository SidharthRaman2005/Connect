
import { GoogleLogin } from "@react-oauth/google";
import { useNavigate } from "react-router-dom";
import { jwtDecode } from "jwt-decode";

function Home() {
  const navigate = useNavigate();

  const handleSuccess = (credentialResponse) => {
    try {
      const user = jwtDecode(credentialResponse.credential);

      if (
        user.email_verified &&
        user.email.endsWith("@vitapstudent.ac.in")
      ) {
        console.log("College login successful!");
        navigate("/about");
      } else {
        alert("Please sign in using your VIT-AP college email.");
      }
    } catch (error) {
      console.error("Unable to verify login:", error);
      alert("Login failed. Please try again.");
    }
  };

  return (
    <div>
      <h1>Welcome to CampusConnect</h1>
      <p>Connect with students who share your interests.</p>

      <GoogleLogin
        onSuccess={handleSuccess}
        onError={() => console.log("Google Login Failed")}
      />
    </div>
  );
}

export default Home;
