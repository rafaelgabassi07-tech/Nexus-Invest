import { execSync } from 'child_process';
try {
  execSync('git checkout -- app/src/main/java/com/example/ui/screens/AnalysisScreen.kt');
  console.log("Restored AnalysisScreen.kt");
} catch (e) {
  console.error(e.message);
}
